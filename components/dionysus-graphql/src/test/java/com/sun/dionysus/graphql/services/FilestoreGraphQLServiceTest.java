package com.sun.dionysus.graphql.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestClient;
import com.sun.dionysus.codegen.types.Bucket;
import com.sun.dionysus.codegen.types.KeyEntry;
import com.sun.dionysus.codegen.types.KeyDetail;
import com.sun.dionysus.codegen.types.RenameKeyResult;
import com.sun.dionysus.graphql.mappers.FileMapper;
import com.sun.dionysus.graphql.mappers.KeyDetailMapper;
import com.sun.dionysus.graphql.mappers.KeyEntryMapper;
import com.sun.dionysus.model.KeyDetailEntity;
import com.sun.dionysus.service.torrent.TorrentJobService;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import com.sun.dionysus.service.KeyDetailService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FilestoreGraphQLServiceTest {

  @Mock
  private RestClient.Builder restClientBuilder;

  @Mock
  private S3Client s3Client;

  @Mock
  private FileMapper fileMapper;

  @Mock
  private KeyEntryMapper keyEntryMapper;

  @Mock
  private KeyDetailMapper keyDetailMapper;

  @Mock
  private KeyDetailService keyDetailService;

  @Mock
  private TorrentJobService torrentJobService;

  @InjectMocks
  private FilestoreGraphQLService filestoreGraphQLService;

  private RestClient restClient;

  @BeforeEach
  void setup() {
    restClient = mock(RestClient.class);
    when(restClientBuilder.build()).thenReturn(restClient);
    filestoreGraphQLService.init();

    when(torrentJobService.findVisibleInBucketUnderPrefix(anyString(), nullable(String.class)))
        .thenReturn(List.of());
    when(keyDetailService.listActiveForBucketAndPath(anyString(), anyString())).thenReturn(List.of());
    when(keyDetailService.createOrUpdateDetail(anyString(), anyString(), any(), any())).thenAnswer(invocation -> {
      KeyDetailEntity detail = new KeyDetailEntity();
      detail.setBucket(invocation.getArgument(0));
      detail.setKeyPath(invocation.getArgument(1));
      return detail;
    });
    when(keyDetailService.locateByBucketAndKeyPath(anyString(), anyString())).thenReturn(Optional.empty());
  }

  @Test
  void health_returnsResponseFromRestApi() {
    String expected = "{\"status\":\"ok\"}";
    stubRestGet(expected, String.class);

    String result = filestoreGraphQLService.health();

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void listBuckets_returnsBucketsFromRestApi() {
    Bucket bucket = new Bucket();
    bucket.setId("b1");
    stubRestGet(new Bucket[] { bucket }, Bucket[].class);

    List<Bucket> result = filestoreGraphQLService.listBuckets();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo("b1");
  }

  @Test
  void putKey_withExplicitName_createsDirectoryAsIs() {
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(PutObjectResponse.builder().build());

    boolean result = filestoreGraphQLService.putKey("default-bucket", "explicit-folder");

    assertThat(result).isTrue();

    ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
    assertThat(captor.getValue().key()).isEqualTo("explicit-folder/");
    verify(s3Client, times(0)).listObjectsV2(any(ListObjectsV2Request.class));
  }

  @Test
  void putKey_withNullKey_noConflict_createsRootNewKey() {
    when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
        .thenReturn(ListObjectsV2Response.builder().build());
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(PutObjectResponse.builder().build());

    boolean result = filestoreGraphQLService.putKey("default-bucket", null);

    assertThat(result).isTrue();

    ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
    assertThat(captor.getValue().key()).isEqualTo("new-key/");
  }

  @Test
  void putKey_withEmptyString_noConflict_createsRootNewKey() {
    when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
        .thenReturn(ListObjectsV2Response.builder().build());
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(PutObjectResponse.builder().build());

    boolean result = filestoreGraphQLService.putKey("default-bucket", "   ");

    assertThat(result).isTrue();

    ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
    assertThat(captor.getValue().key()).isEqualTo("new-key/");
  }

  @Test
  void putKey_withParentPath_hasConflicts_createsIncrementedNewKey() {
    ListObjectsV2Response hit0 = ListObjectsV2Response.builder()
        .contents(S3Object.builder().key("parent/new-key/").build())
        .build();
    ListObjectsV2Response hit1 = ListObjectsV2Response.builder()
        .contents(S3Object.builder().key("parent/new-key-1/").build())
        .build();
    ListObjectsV2Response empty = ListObjectsV2Response.builder().build();

    when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
        .thenReturn(hit0, hit1, empty);
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(PutObjectResponse.builder().build());

    boolean result = filestoreGraphQLService.putKey("default-bucket", "parent/");

    assertThat(result).isTrue();

    ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
    assertThat(captor.getValue().key()).isEqualTo("parent/new-key-2/");
    verify(s3Client, times(3)).listObjectsV2(any(ListObjectsV2Request.class));
  }

  @Test
  void deleteFile_returnsTrue() {
    when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
        .thenReturn(DeleteObjectResponse.builder().build());

    boolean result = filestoreGraphQLService.deleteFile("default-bucket", "test.txt");

    assertThat(result).isTrue();
    verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
  }

  @Test
  void listKeys_returnsDirectoryAndFileEntries() {
    CommonPrefix prefix = CommonPrefix.builder().prefix("dir/").build();
    S3Object file = S3Object.builder()
        .key("dir/file.txt")
        .size(42L)
        .lastModified(Instant.parse("2024-01-01T10:00:00Z"))
        .build();
    ListObjectsV2Response resp = ListObjectsV2Response.builder()
        .commonPrefixes(prefix)
        .contents(file)
        .build();

    KeyDetailEntity dirDetail = new KeyDetailEntity();
    dirDetail.setKeyPath("dir/");
    dirDetail.setName("My Directory");
    dirDetail.setDescription("Directory description");

    KeyDetailEntity fileDetail = new KeyDetailEntity();
    fileDetail.setKeyPath("dir/file.txt");
    fileDetail.setName("My File");
    fileDetail.setDescription("File description");

    when(keyDetailService.listActiveForBucketAndPath("bucket", "dir/"))
        .thenReturn(List.of(dirDetail, fileDetail));
    ListObjectsV2Iterable paginator = pagesPaginator(resp);
    when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class))).thenReturn(paginator);

    KeyEntry dirEntry = new KeyEntry();
    dirEntry.setKey("dir/");
    dirEntry.setIsDirectory(true);
    dirEntry.setName("My Directory");
    dirEntry.setDescription("Directory description");

    KeyEntry fileEntry = new KeyEntry();
    fileEntry.setKey("dir/file.txt");
    fileEntry.setIsDirectory(false);
    fileEntry.setSize(42);
    fileEntry.setLastModified("2024-01-01T10:00:00Z");
    fileEntry.setName("My File");
    fileEntry.setDescription("File description");

    when(keyEntryMapper.mapDirectory(any(CommonPrefix.class), nullable(KeyDetailEntity.class))).thenReturn(dirEntry);
    when(keyEntryMapper.mapFile(any(S3Object.class), nullable(KeyDetailEntity.class))).thenReturn(fileEntry);

    List<KeyEntry> result = filestoreGraphQLService.listKeys("bucket", "dir/");

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getKey()).isEqualTo("dir/");
    assertThat(result.get(0).getIsDirectory()).isTrue();
    assertThat(result.get(0).getName()).isEqualTo("My Directory");
    assertThat(result.get(1).getKey()).isEqualTo("dir/file.txt");
    assertThat(result.get(1).getIsDirectory()).isFalse();
    assertThat(result.get(1).getSize()).isEqualTo(42);
    assertThat(result.get(1).getName()).isEqualTo("My File");
  }

  @Test
  void listKeys_withoutKeyDetail_returnsEntriesWithoutMetadata() {
    CommonPrefix prefix = CommonPrefix.builder().prefix("folder/").build();
    S3Object file = S3Object.builder().key("folder/file.txt").size(100L).build();
    ListObjectsV2Response resp = ListObjectsV2Response.builder()
        .commonPrefixes(prefix)
        .contents(file)
        .build();

    when(keyDetailService.listActiveForBucketAndPath("bucket", "folder/")).thenReturn(List.of());
    ListObjectsV2Iterable paginator = pagesPaginator(resp);
    when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class))).thenReturn(paginator);

    KeyEntry dirEntry = new KeyEntry();
    dirEntry.setKey("folder/");
    dirEntry.setIsDirectory(true);

    KeyEntry fileEntry = new KeyEntry();
    fileEntry.setKey("folder/file.txt");
    fileEntry.setIsDirectory(false);
    fileEntry.setSize(100);

    when(keyEntryMapper.mapDirectory(any(CommonPrefix.class), nullable(KeyDetailEntity.class))).thenReturn(dirEntry);
    when(keyEntryMapper.mapFile(any(S3Object.class), nullable(KeyDetailEntity.class))).thenReturn(fileEntry);

    List<KeyEntry> result = filestoreGraphQLService.listKeys("bucket", "folder/");

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isNull();
    assertThat(result.get(1).getName()).isNull();
  }

  @Test
  void deleteKey_returnsTrue() {
    ListObjectsV2Iterable paginator = contentsPaginator();
    when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class))).thenReturn(paginator);
    when(s3Client.deleteObjects(any(DeleteObjectsRequest.class)))
        .thenReturn(DeleteObjectsResponse.builder().build());

    boolean result = filestoreGraphQLService.deleteKey("default-bucket", "folder/");

    assertThat(result).isTrue();
    verify(s3Client).deleteObjects(any(DeleteObjectsRequest.class));
  }

  @Test
  void deleteKey_deletesExactAndSubKeys() {
    S3Object marker = S3Object.builder().key("dir/").build();
    S3Object child = S3Object.builder().key("dir/child.txt").build();
    ListObjectsV2Iterable paginator = contentsPaginator(marker, child);
    when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class))).thenReturn(paginator);
    when(s3Client.deleteObjects(any(DeleteObjectsRequest.class)))
        .thenReturn(DeleteObjectsResponse.builder().build());

    boolean result = filestoreGraphQLService.deleteKey("bucket", "dir");

    assertThat(result).isTrue();
    verify(s3Client).deleteObjects(any(DeleteObjectsRequest.class));
  }

  @Test
  void deleteKey_handlesPaginationAndBatch() {
    S3Object a = S3Object.builder().key("big/a").build();
    S3Object b = S3Object.builder().key("big/b").build();
    ListObjectsV2Iterable paginator = contentsPaginator(a, b);
    when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class))).thenReturn(paginator);
    when(s3Client.deleteObjects(any(DeleteObjectsRequest.class)))
        .thenReturn(DeleteObjectsResponse.builder().build());

    boolean result = filestoreGraphQLService.deleteKey("bkt", "big");

    assertThat(result).isTrue();
    verify(s3Client).deleteObjects(any(DeleteObjectsRequest.class));
  }

  /**
   * Even with no child objects, the key marker itself is still deleted.
   */
  @Test
  void deleteKey_withNoChildObjects_deletesKeyItself() {
    ListObjectsV2Iterable paginator = contentsPaginator();
    when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class))).thenReturn(paginator);
    when(s3Client.deleteObjects(any(DeleteObjectsRequest.class)))
        .thenReturn(DeleteObjectsResponse.builder().build());

    boolean result = filestoreGraphQLService.deleteKey("bucket", "emptykey");

    assertThat(result).isTrue();
    verify(s3Client).deleteObjects(any(DeleteObjectsRequest.class));
  }

  @Test
  void renameKey_noSourceObjects_returnsFalse() {
    ListObjectsV2Iterable paginator = contentsPaginator();
    when(s3Client.listObjectsV2Paginator(any(Consumer.class))).thenReturn(paginator);

    RenameKeyResult result = filestoreGraphQLService.renameKey("bucket", "old.txt", "new.txt", false);

    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getHasConflicts()).isFalse();
    assertThat(result.getConflicts()).isEmpty();
  }

  @Test
  void renameKey_withoutMergeAndHasConflicts_returnsConflicts() {
    S3Object sourceObj = S3Object.builder().key("old.txt").build();
    ListObjectsV2Iterable paginator = contentsPaginator(sourceObj);
    when(s3Client.listObjectsV2Paginator(any(Consumer.class))).thenReturn(paginator);
    when(s3Client.headObject(any(Consumer.class))).thenReturn(HeadObjectResponse.builder().build());

    RenameKeyResult result = filestoreGraphQLService.renameKey("bucket", "old.txt", "new.txt", false);

    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getHasConflicts()).isTrue();
    assertThat(result.getConflicts()).containsExactly("new.txt");
  }

  @Test
  void renameKey_withMergeTrue_bypassesConflictsAndExecutesMove() {
    S3Object sourceObj = S3Object.builder().key("old.txt").build();
    ListObjectsV2Iterable paginator = contentsPaginator(sourceObj);
    when(s3Client.listObjectsV2Paginator(any(Consumer.class))).thenReturn(paginator);
    when(s3Client.copyObject(any(CopyObjectRequest.class))).thenReturn(CopyObjectResponse.builder().build());
    when(s3Client.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(DeleteObjectsResponse.builder().build());

    RenameKeyResult result = filestoreGraphQLService.renameKey("bucket", "old.txt", "new.txt", true);

    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getHasConflicts()).isFalse();
    verify(s3Client).copyObject(any(CopyObjectRequest.class));
    verify(s3Client).deleteObjects(any(DeleteObjectsRequest.class));
  }

  @Test
  void locate_returnsMappedKeyDetail() {
    KeyDetailEntity entity = new KeyDetailEntity();
    entity.setBucket("my-bucket");
    entity.setKeyPath("documents/report.pdf");
    entity.setName("Quarterly Report");
    entity.setDescription("Q4 financial summary");

    KeyDetail expected = new KeyDetail();
    expected.setBucket("my-bucket");
    expected.setKeyPath("documents/report.pdf");
    expected.setName("Quarterly Report");
    expected.setDescription("Q4 financial summary");

    when(keyDetailService.locateByBucketAndKeyPath("my-bucket", "documents/report.pdf"))
        .thenReturn(Optional.of(entity));
    when(keyDetailMapper.map(entity)).thenReturn(expected);

    KeyDetail result = filestoreGraphQLService.locate("my-bucket", "documents/report.pdf");

    assertThat(result).isNotNull();
    assertThat(result.getBucket()).isEqualTo("my-bucket");
    assertThat(result.getKeyPath()).isEqualTo("documents/report.pdf");
    assertThat(result.getName()).isEqualTo("Quarterly Report");
  }

  @Test
  void locate_returnsNullWhenNotFound() {
    when(keyDetailService.locateByBucketAndKeyPath("my-bucket", "missing.txt"))
        .thenReturn(Optional.empty());

    KeyDetail result = filestoreGraphQLService.locate("my-bucket", "missing.txt");

    assertThat(result).isNull();
  }

  /**
   * Stubs the RestClient GET fluent chain to return the given body.
   */
  @SuppressWarnings("unchecked")
  private <T> void stubRestGet(T body, Class<T> responseType) {
    RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
    RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
    when(restClient.get()).thenReturn(uriSpec);
    when(uriSpec.uri(anyString())).thenReturn(uriSpec);
    when(uriSpec.header(anyString(), any(String[].class))).thenReturn(uriSpec);
    when(uriSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.body(responseType)).thenReturn(body);
  }

  /**
   * Builds a paginator mock whose page iterator yields the given response pages.
   */
  private ListObjectsV2Iterable pagesPaginator(ListObjectsV2Response... pages) {
    ListObjectsV2Iterable paginator = mock(ListObjectsV2Iterable.class);
    List<ListObjectsV2Response> pageList = List.of(pages);
    when(paginator.iterator()).thenReturn(pageList.iterator());
    return paginator;
  }

  /**
   * Builds a paginator mock whose contents stream yields the given objects.
   */
  private ListObjectsV2Iterable contentsPaginator(S3Object... objects) {
    ListObjectsV2Iterable paginator = mock(ListObjectsV2Iterable.class);
    List<S3Object> contents = List.of(objects);
    when(paginator.contents()).thenReturn(() -> contents.iterator());
    return paginator;
  }
}
