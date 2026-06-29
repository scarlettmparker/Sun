package com.sun.dionysus.service;

import com.sun.dionysus.graphql.models.KeyDetailEntity;
import com.sun.dionysus.graphql.models.Status;
import com.sun.dionysus.service.repository.KeyDetailEntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;

/**
 * Unit tests for KeyDetailService.
 */
@ExtendWith(MockitoExtension.class)
class KeyDetailServiceTest {

  @Mock
  private KeyDetailEntityRepository repository;

  @InjectMocks
  private KeyDetailService service;

  @Test
  void createOrUpdateDetail_createsWhenNotExists() {
    when(repository.findByBucketAndKeyPath(anyString(), anyString())).thenReturn(List.of());
    when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

    KeyDetailEntity created = service.createOrUpdateDetail("bkt", "path/file.txt", "file.txt", "text/plain");

    assertThat(created.getBucket()).isEqualTo("bkt");
    assertThat(created.getKeyPath()).isEqualTo("path/file.txt");
    assertThat(created.getName()).isEqualTo("file.txt");
    assertThat(created.getStatus()).isEqualTo(Status.ACTIVE);
    assertThat(created.getContentType()).isEqualTo("text/plain");
  }

  @Test
  void archiveDetail_marksAsArchived() {
    KeyDetailEntity d = new KeyDetailEntity();
    d.setBucket("b");
    d.setKeyPath("k");
    when(repository.findByBucketAndKeyPath("b", "k")).thenReturn(List.of(d));

    service.archiveDetail("b", "k");

    verify(repository).save(d);
    assertThat(d.getStatus()).isEqualTo(Status.ARCHIVED);
    assertThat(d.getArchivedAt()).isNotNull();
  }

  @Test
  void listActiveForBucketAndPath_returnsActiveRecords() {
    KeyDetailEntity d = new KeyDetailEntity();
    d.setStatus(Status.ACTIVE);
    when(repository.findByBucketAndKeyPathStartingWithAndStatus("bkt", "dir/", Status.ACTIVE))
        .thenReturn(List.of(d));

    List<KeyDetailEntity> result = service.listActiveForBucketAndPath("bkt", "dir/");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getStatus()).isEqualTo(Status.ACTIVE);
  }

  @Test
  void updatePath_updatesKeyPaths() {
    KeyDetailEntity d = new KeyDetailEntity();
    d.setKeyPath("old/file.txt");
    when(repository.findByBucketAndKeyPathStartingWith("bkt", "old/")).thenReturn(List.of(d));

    service.updatePath("bkt", "old", "new");

    verify(repository).save(d);
    assertThat(d.getKeyPath()).isEqualTo("new/file.txt");
  }

  @Test
  void locateByBucketAndKeyPath_returnsActiveRecord() {
    KeyDetailEntity d = new KeyDetailEntity();
    d.setBucket("bkt");
    d.setKeyPath("dir/file.txt");
    d.setName("file.txt");
    d.setStatus(Status.ACTIVE);
    when(repository.findByBucketAndKeyPathAndStatus("bkt", "dir/file.txt", Status.ACTIVE))
        .thenReturn(Optional.of(d));

    Optional<KeyDetailEntity> result = service.locateByBucketAndKeyPath("bkt", "dir/file.txt");

    assertThat(result).isPresent();
    assertThat(result.get().getBucket()).isEqualTo("bkt");
    assertThat(result.get().getKeyPath()).isEqualTo("dir/file.txt");
    assertThat(result.get().getName()).isEqualTo("file.txt");
  }

  @Test
  void locateByBucketAndKeyPath_returnsEmptyWhenNotFound() {
    when(repository.findByBucketAndKeyPathAndStatus("bkt", "missing.txt", Status.ACTIVE))
        .thenReturn(Optional.empty());

    Optional<KeyDetailEntity> result = service.locateByBucketAndKeyPath("bkt", "missing.txt");

    assertThat(result).isEmpty();
  }
}
