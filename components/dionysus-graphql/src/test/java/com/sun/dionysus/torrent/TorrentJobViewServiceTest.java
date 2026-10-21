package com.sun.dionysus.torrent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.dionysus.codegen.types.KeyEntry;
import com.sun.dionysus.codegen.types.TorrentJob;
import com.sun.dionysus.codegen.types.TorrentJobStatus;
import com.sun.dionysus.graphql.mappers.KeyEntryMapper;
import com.sun.dionysus.graphql.mappers.TorrentJobMapper;
import com.sun.dionysus.model.TorrentJobEntity;
import com.sun.dionysus.model.enums.TorrentStatus;
import com.sun.dionysus.service.torrent.TorrentJobService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TorrentJobViewServiceTest {

  @Mock private TorrentJobService jobService;
  @Mock private TorrentJobMapper jobMapper;
  @Mock private KeyEntryMapper keyEntryMapper;

  @InjectMocks private TorrentJobViewService view;

  @Test
  void byId_shouldLoadAndMap() {
    UUID id = UUID.randomUUID();
    TorrentJobEntity entity = job("b", TorrentStatus.DOWNLOADING);
    entity.setId(id);
    TorrentJob mapped = mapped(entity);
    when(jobService.findById(id)).thenReturn(Optional.of(entity));
    when(jobMapper.map(entity)).thenReturn(mapped);

    Optional<TorrentJob> result = view.byId(id);

    assertThat(result).contains(mapped);
  }

  @Test
  void byId_shouldReturnEmptyWhenNotFound() {
    UUID id = UUID.randomUUID();
    when(jobService.findById(id)).thenReturn(Optional.empty());

    assertThat(view.byId(id)).isEmpty();
  }

  @Test
  void list_shouldFilterByBucketAndStatus() {
    TorrentJobEntity match = job("bucket", TorrentStatus.DOWNLOADING);
    TorrentJobEntity otherBucket = job("other", TorrentStatus.DOWNLOADING);
    TorrentJobEntity otherStatus = job("bucket", TorrentStatus.PAUSED);
    TorrentJob matchMapped = mapped(match);
    when(jobService.findAll()).thenReturn(List.of(match, otherBucket, otherStatus));
    when(jobMapper.map(match)).thenReturn(matchMapped);

    List<TorrentJob> result = view.list("bucket", TorrentStatus.DOWNLOADING);

    assertThat(result).containsExactly(matchMapped);
  }

  @Test
  void list_shouldReturnAllWhenNoFilter() {
    TorrentJobEntity e1 = job("b1", TorrentStatus.DOWNLOADING);
    TorrentJobEntity e2 = job("b2", TorrentStatus.PAUSED);
    when(jobService.findAll()).thenReturn(List.of(e1, e2));
    when(jobMapper.map(any(TorrentJobEntity.class)))
        .thenAnswer(
            inv -> {
              TorrentJobEntity e = inv.getArgument(0);
              return mapped(e);
            });

    List<TorrentJob> result = view.list(null, null);

    assertThat(result).hasSize(2);
  }

  @Test
  void mergeInProgressJobs_shouldMergeExistingAndAddSynthetic() {
    TorrentJobEntity existingJob = job("bucket", TorrentStatus.DOWNLOADING);
    existingJob.setTargetKeyPath("existing");
    TorrentJobEntity newJob = job("bucket", TorrentStatus.DOWNLOADING);
    newJob.setTargetKeyPath("new");
    when(jobService.findVisibleInBucketUnderPrefix("bucket", "p/"))
        .thenReturn(List.of(existingJob, newJob));

    KeyEntry synthetic = KeyEntry.newBuilder().key("new").isDirectory(false).size(0).build();
    when(keyEntryMapper.mapTorrentJob(newJob)).thenReturn(synthetic);

    Map<String, KeyEntry> entriesByKey = new LinkedHashMap<>();
    KeyEntry existing = KeyEntry.newBuilder().key("existing").isDirectory(false).size(10).build();
    entriesByKey.put("existing", existing);

    List<KeyEntry> result = view.mergeInProgressJobs("bucket", "p/", entriesByKey);

    verify(keyEntryMapper, times(1)).mergeTorrentJob(existing, existingJob);
    assertThat(result).containsExactly(synthetic);
    assertThat(entriesByKey).containsKeys("existing", "new");
  }

  @Test
  void mergeInProgressJobs_shouldIgnoreJobsOutsidePrefix() {
    when(jobService.findVisibleInBucketUnderPrefix(any(), any())).thenReturn(List.of());

    List<KeyEntry> result = view.mergeInProgressJobs("bucket", "p/", Map.of());

    assertThat(result).isEmpty();
  }

  private TorrentJobEntity job(String bucket, TorrentStatus status) {
    TorrentJobEntity e = new TorrentJobEntity();
    e.setId(UUID.randomUUID());
    e.setBucket(bucket);
    e.setTargetKeyPath("k");
    e.setSourceType("MAGNET");
    e.setStatus(status);
    e.setInfoHash("h");
    e.setTotalBytes(0L);
    e.setDownloadedBytes(0L);
    e.setUploadedBytes(0L);
    e.setProgress(0.0);
    return e;
  }

  private TorrentJob mapped(TorrentJobEntity entity) {
    return TorrentJob.newBuilder()
        .id(entity.getId().toString())
        .bucket(entity.getBucket())
        .targetKeyPath(entity.getTargetKeyPath())
        .sourceType("MAGNET")
        .status(TorrentJobStatus.valueOf(entity.getStatus().name()))
        .infoHash("h")
        .totalBytes(0L)
        .downloadedBytes(0L)
        .uploadedBytes(0L)
        .progress(0.0)
        .build();
  }
}
