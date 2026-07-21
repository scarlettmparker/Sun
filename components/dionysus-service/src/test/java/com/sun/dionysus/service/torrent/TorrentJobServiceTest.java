package com.sun.dionysus.service.torrent;

import com.sun.dionysus.model.TorrentJobEntity;
import com.sun.dionysus.model.enums.TorrentStatus;
import com.sun.dionysus.repository.TorrentJobEntityRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TorrentJobService.
 */
@ExtendWith(MockitoExtension.class)
class TorrentJobServiceTest {

  @Mock
  private TorrentJobEntityRepository repository;

  @InjectMocks
  private TorrentJobService service;

  @Test
  void findByInfoHash_delegates() {
    TorrentJobEntity job = newJob("abc123", "bucket", "movie.mp4");
    when(repository.findByInfoHash("abc123")).thenReturn(Optional.of(job));

    Optional<TorrentJobEntity> result = service.findByInfoHash("abc123");

    assertThat(result).contains(job);
  }

  @Test
  void hasActiveAt_returnsTrueWhenVisibleJobTargetsKey() {
    TorrentJobEntity job = newJob("h", "bucket", "dir/file.txt");
    job.setStatus(TorrentStatus.DOWNLOADING);
    when(repository.findByBucketAndStatusIn("bucket", TorrentJobService.VISIBLE_STATUSES))
        .thenReturn(List.of(job));

    assertThat(service.hasActiveAt("bucket", "dir/file.txt")).isTrue();
  }

  @Test
  void hasActiveAt_returnsFalseWhenCompletedJobTargetsKey() {
    TorrentJobEntity job = newJob("h", "bucket", "dir/file.txt");
    job.setStatus(TorrentStatus.COMPLETED);
    when(repository.findByBucketAndStatusIn("bucket", TorrentJobService.VISIBLE_STATUSES))
        .thenReturn(List.of());

    assertThat(service.hasActiveAt("bucket", "dir/file.txt")).isFalse();
  }

  @Test
  void findResumable_delegatesWithResumableStatuses() {
    TorrentJobEntity job = newJob("h", "bucket", "k");
    job.setStatus(TorrentStatus.DOWNLOADING);
    when(repository.findByStatusIn(TorrentJobService.RESUMABLE_STATUSES)).thenReturn(List.of(job));

    List<TorrentJobEntity> result = service.findResumable();

    assertThat(result).contains(job);
  }

  @Test
  void findVisibleInBucketUnderPrefix_returnsOnlyDirectChildren() {
    TorrentJobEntity direct = newJob("h1", "bucket", "dir/movie.mp4");
    TorrentJobEntity nested = newJob("h2", "bucket", "dir/sub/deep.mp4");
    TorrentJobEntity other = newJob("h3", "bucket", "dir2/x.mp4");
    when(repository.findByBucketAndStatusIn("bucket", TorrentJobService.VISIBLE_STATUSES))
        .thenReturn(List.of(direct, nested, other));

    List<TorrentJobEntity> result = service.findVisibleInBucketUnderPrefix("bucket", "dir/");

    assertThat(result).containsExactly(direct);
  }

  @Test
  void findVisibleInBucketUnderPrefix_rootReturnsTopLevelOnly() {
    TorrentJobEntity top = newJob("h1", "bucket", "movie.mp4");
    TorrentJobEntity folder = newJob("h2", "bucket", "album/");
    TorrentJobEntity nested = newJob("h3", "bucket", "sub/deep.mp4");
    when(repository.findByBucketAndStatusIn("bucket", TorrentJobService.VISIBLE_STATUSES))
        .thenReturn(List.of(top, folder, nested));

    List<TorrentJobEntity> result = service.findVisibleInBucketUnderPrefix("bucket", "");

    assertThat(result).containsExactlyInAnyOrder(top, folder);
  }

  @Test
  void markAllPaused_setsPausedStatusAndTimestamp() {
    UUID id = UUID.randomUUID();
    TorrentJobEntity job = newJob("h", "bucket", "k");
    job.setStatus(TorrentStatus.DOWNLOADING);
    when(repository.findById(id)).thenReturn(Optional.of(job));
    when(repository.save(any(TorrentJobEntity.class))).thenAnswer(i -> i.getArgument(0));

    service.markAllPaused(List.of(id));

    assertThat(job.getStatus()).isEqualTo(TorrentStatus.PAUSED);
    assertThat(job.getPausedAt()).isNotNull();
    verify(repository).save(job);
  }

  private TorrentJobEntity newJob(String infoHash, String bucket, String targetKeyPath) {
    TorrentJobEntity job = new TorrentJobEntity();
    job.setInfoHash(infoHash);
    job.setBucket(bucket);
    job.setTargetKeyPath(targetKeyPath);
    return job;
  }
}
