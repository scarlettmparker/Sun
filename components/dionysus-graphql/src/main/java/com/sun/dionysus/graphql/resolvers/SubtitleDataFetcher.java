package com.sun.dionysus.graphql.resolvers;

import com.sun.base.audit.annotation.Audited;
import com.sun.dionysus.codegen.types.DeleteSubtitleInput;
import com.sun.dionysus.codegen.types.DeleteSubtitleResponse;
import com.sun.dionysus.codegen.types.FetchSubtitleInput;
import com.sun.dionysus.codegen.types.FetchSubtitleResponse;
import com.sun.dionysus.codegen.types.SearchSubtitlesInput;
import com.sun.dionysus.codegen.types.Subtitle;
import com.sun.dionysus.codegen.types.SubtitleSearchResult;
import com.sun.dionysus.codegen.types.SubtitleSource;
import com.sun.dionysus.graphql.mappers.SubtitleMapper;
import com.sun.dionysus.model.SubtitleEntity;
import com.sun.dionysus.service.SubtitleService;
import com.sun.dionysus.subtitle.MovieNameParser;
import com.sun.dionysus.subtitle.OpenSubtitlesHasher;
import com.sun.dionysus.subtitle.OpenSubtitlesService;
import com.sun.dionysus.subtitle.SrtToVtt;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.core.sync.RequestBody;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.InputArgument;
import com.sun.base.error.MutationException;

/**
 * Resolves subtitle queries and mutations.
 */
@DgsComponent
public class SubtitleDataFetcher {

  private static final Logger logger = LoggerFactory.getLogger(SubtitleDataFetcher.class);
  private static final Set<String> SUBTITLE_EXTENSIONS = Set.of("srt", "vtt", "ass", "ssa");

  private final S3Client s3Client;
  private final SubtitleService subtitleService;
  private final OpenSubtitlesService openSubtitlesService;
  private final SubtitleMapper subtitleMapper;

  /**
   * Creates a subtitle data fetcher.
   *
   * @param s3Client the S3 client
   * @param subtitleService the subtitle service
   * @param openSubtitlesService the OpenSubtitles client
   * @param subtitleMapper the subtitle mapper
   */
  public SubtitleDataFetcher(
      S3Client s3Client,
      SubtitleService subtitleService,
      OpenSubtitlesService openSubtitlesService,
      SubtitleMapper subtitleMapper) {
    this.s3Client = s3Client;
    this.subtitleService = subtitleService;
    this.openSubtitlesService = openSubtitlesService;
    this.subtitleMapper = subtitleMapper;
  }

  @Audited(event = "SUBTITLES")
  @PreAuthorize("@permissions.has('graphql.dionysus.subtitles')")
  @DgsData(parentType = "FilestoreQueries", field = "subtitles")
  public List<Subtitle> subtitles(
      @InputArgument("bucket") String bucket, @InputArgument("keyPath") String keyPath) {
    List<Subtitle> result = new ArrayList<>();
    for (SubtitleEntity entity : subtitleService.listForVideo(bucket, keyPath)) {
      result.add(subtitleMapper.map(entity));
    }
    result.addAll(listLocalSubtitles(bucket, keyPath));
    return result;
  }

  @Audited(event = "SEARCHSUBTITLES")
  @PreAuthorize("@permissions.has('graphql.dionysus.searchSubtitles')")
  @DgsData(parentType = "FilestoreMutations", field = "searchSubtitles")
  public List<SubtitleSearchResult> searchSubtitles(@InputArgument("input") SearchSubtitlesInput input) {
    String bucket = input.getBucket();
    String keyPath = input.getKeyPath();
    String language = normalizeLanguage(input.getLanguage());
    String query = input.getQuery();
    MovieNameParser.ParsedMovieName parsed = MovieNameParser.parse(keyPath);
    if (query == null || query.isBlank()) {
      query = parsed.getQuery();
    }
    Integer year = parsed.getYear();

    String hash = null;
    Long size = null;
    try {
      HeadObjectResponse head = s3Client.headObject(HeadObjectRequest.builder()
          .bucket(bucket)
          .key(keyPath)
          .build());
      size = head.contentLength();
      if (size != null && size > 0) {
        long chunk = Math.min(65536L, size);
        byte[] first = getObjectRange(bucket, keyPath, 0, chunk - 1);
        byte[] last = size <= 65536
            ? first
            : getObjectRange(bucket, keyPath, size - 65536, size - 1);
        hash = OpenSubtitlesHasher.hash(size, first, last);
      }
    } catch (Exception e) {
      logger.debug("Could not hash {}: {}", keyPath, e.getMessage());
    }

    List<com.sun.dionysus.subtitle.SubtitleSearchResult> serviceResults =
        openSubtitlesService.search(query, year, language, hash, size);

    List<SubtitleSearchResult> results = new ArrayList<>();
    for (com.sun.dionysus.subtitle.SubtitleSearchResult serviceResult : serviceResults) {
      results.add(SubtitleSearchResult.newBuilder()
          .source(mapSource(serviceResult.getSource()))
          .subtitleKey(serviceResult.getSubtitleKey())
          .fileId(serviceResult.getFileId())
          .language(serviceResult.getLanguage())
          .label(serviceResult.getLabel())
          .hearingImpaired(serviceResult.getHearingImpaired())
          .downloads(serviceResult.getDownloads())
          .rating(serviceResult.getRating())
          .build());
    }
    return results;
  }

  @Audited(event = "FETCHSUBTITLE")
  @PreAuthorize("@permissions.has('graphql.dionysus.fetchSubtitle')")
  @DgsData(parentType = "FilestoreMutations", field = "fetchSubtitle")
  public FetchSubtitleResponse fetchSubtitle(@InputArgument("input") FetchSubtitleInput input) {
    String bucket = input.getBucket();
    String keyPath = input.getKeyPath();
    String language = normalizeLanguage(input.getLanguage());
    SubtitleSource source = input.getSource();

    if (source == SubtitleSource.LOCAL) {
      String subtitleKey = input.getSubtitleKey();
      byte[] data = getObjectBytes(bucket, subtitleKey);
      String vtt;
      try {
        vtt = SrtToVtt.convert(data, subtitleKey);
      } catch (Exception e) {
        throw new MutationException("fetchSubtitle failed: " + e.getMessage(), e);
      }
      return FetchSubtitleResponse.newBuilder()
          .vtt(vtt)
          .subtitleKey(subtitleKey)
          .build();
    }

    Integer fileId = input.getFileId();
    byte[] data = openSubtitlesService.download(fileId);
    if (data == null) {
      throw new MutationException("fetchSubtitle failed: download returned no data");
    }
    String vtt;
    try {
      vtt = SrtToVtt.convert(data, "subtitle.srt");
    } catch (Exception e) {
      throw new MutationException("fetchSubtitle failed: " + e.getMessage(), e);
    }

    String subtitleKey = null;
    if (Boolean.TRUE.equals(input.getPersist())) {
      try {
        String stem = stemOf(keyPath);
        String parent = parentPrefix(keyPath);
        String targetKey = parent + stem + "." + language + ".srt";
        putObject(bucket, targetKey, data);
        subtitleService.attach(bucket, keyPath, targetKey, language,
            com.sun.dionysus.model.enums.SubtitleSource.OPENSUBTITLES, fileId);
        subtitleKey = targetKey;
      } catch (Exception e) {
        logger.warn("Failed to persist subtitle for {}: {}", keyPath, e.getMessage());
      }
    }

    return FetchSubtitleResponse.newBuilder()
        .vtt(vtt)
        .subtitleKey(subtitleKey)
        .build();
  }

  @Audited(event = "DELETESUBTITLE")
  @PreAuthorize("@permissions.has('graphql.dionysus.deleteSubtitle')")
  @DgsData(parentType = "FilestoreMutations", field = "deleteSubtitle")
  public DeleteSubtitleResponse deleteSubtitle(@InputArgument("input") DeleteSubtitleInput input) {
    String bucket = input.getBucket();
    String subtitleKey = input.getSubtitleKey();
    try {
      s3Client.deleteObject(DeleteObjectRequest.builder()
          .bucket(bucket)
          .key(subtitleKey)
          .build());
    } catch (Exception e) {
      logger.debug("S3 delete failed for {}: {}", subtitleKey, e.getMessage());
    }
    subtitleService.deleteBySubtitleKey(bucket, subtitleKey);
    return DeleteSubtitleResponse.newBuilder()
        .message("deleteSubtitle succeeded")
        .key(subtitleKey)
        .build();
  }

  private List<Subtitle> listLocalSubtitles(String bucket, String keyPath) {
    List<Subtitle> result = new ArrayList<>();
    String parent = parentPrefix(keyPath);
    String stem = stemOf(keyPath);
    Set<String> existingKeys = new java.util.HashSet<>();
    for (SubtitleEntity entity : subtitleService.listForVideo(bucket, keyPath)) {
      existingKeys.add(entity.getSubtitleKey());
    }

    ListObjectsV2Request request = ListObjectsV2Request.builder()
        .bucket(bucket)
        .prefix(parent)
        .build();
    var paginator = s3Client.listObjectsV2Paginator(request);
    if (paginator == null) {
      return result;
    }
    for (S3Object object : paginator.contents()) {
      String key = object.key();
      if (existingKeys.contains(key)) {
        continue;
      }
      String name = basename(key);
      String ext = extension(name);
      if (!SUBTITLE_EXTENSIONS.contains(ext)) {
        continue;
      }
      String base = name.substring(0, name.length() - ext.length() - 1);
      if (!base.equals(stem) && !base.startsWith(stem + ".")) {
        continue;
      }
      String language = inferLanguage(base, stem);
      result.add(Subtitle.newBuilder()
          .bucket(bucket)
          .keyPath(keyPath)
          .subtitleKey(key)
          .language(language)
          .source(SubtitleSource.LOCAL)
          .build());
    }
    return result;
  }

  private byte[] getObjectBytes(String bucket, String key) {
    try (ResponseInputStream<?> stream = s3Client.getObject(GetObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build())) {
      return stream.readAllBytes();
    } catch (Exception e) {
      throw new MutationException("Failed to read " + key + ": " + e.getMessage(), e);
    }
  }

  private byte[] getObjectRange(String bucket, String key, long start, long end) {
    try (ResponseInputStream<?> stream = s3Client.getObject(GetObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .range("bytes=" + start + "-" + end)
        .build())) {
      return stream.readAllBytes();
    } catch (Exception e) {
      throw new MutationException("Failed to read range from " + key + ": " + e.getMessage(), e);
    }
  }

  private void putObject(String bucket, String key, byte[] data) {
    s3Client.putObject(PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .contentType("text/plain; charset=utf-8")
        .build(), RequestBody.fromBytes(data));
  }

  private static SubtitleSource mapSource(com.sun.dionysus.model.enums.SubtitleSource source) {
    return SubtitleSource.valueOf(source.name());
  }

  private static String normalizeLanguage(String language) {
    if (language == null || language.isBlank()) {
      return "en";
    }
    return language.trim().toLowerCase(Locale.ROOT);
  }

  private static String inferLanguage(String base, String stem) {
    String remainder = base.substring(stem.length());
    if (remainder.startsWith(".")) {
      String lang = remainder.substring(1);
      if (!lang.isBlank()) {
        return lang;
      }
    }
    return "en";
  }

  private static String parentPrefix(String key) {
    int slash = key.lastIndexOf('/');
    return slash >= 0 ? key.substring(0, slash + 1) : "";
  }

  private static String stemOf(String key) {
    String name = basename(key);
    int dot = name.lastIndexOf('.');
    return dot > 0 ? name.substring(0, dot) : name;
  }

  private static String basename(String key) {
    int slash = key.lastIndexOf('/');
    return slash >= 0 ? key.substring(slash + 1) : key;
  }

  private static String extension(String name) {
    int dot = name.lastIndexOf('.');
    if (dot < 0 || dot == name.length() - 1) {
      return "";
    }
    return name.substring(dot + 1).toLowerCase(Locale.ROOT);
  }
}
