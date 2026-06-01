package com.sun.dionysus.service;

import com.sun.dionysus.graphql.models.KeyDetail;
import com.sun.dionysus.graphql.models.Status;
import com.sun.dionysus.service.repository.KeyDetailRepository;
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
  private KeyDetailRepository repository;

  @InjectMocks
  private KeyDetailService service;

  @Test
  void createOrUpdateDetail_createsWhenNotExists() {
    when(repository.findByBucketAndKeyPath(anyString(), anyString())).thenReturn(List.of());

    KeyDetail created = service.createOrUpdateDetail("bkt", "path/file.txt", "file.txt");

    assertThat(created.getBucket()).isEqualTo("bkt");
    assertThat(created.getKeyPath()).isEqualTo("path/file.txt");
    assertThat(created.getName()).isEqualTo("file.txt");
    assertThat(created.getStatus()).isEqualTo(Status.ACTIVE);
  }

  @Test
  void archiveDetail_marksAsArchived() {
    KeyDetail d = new KeyDetail();
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
    KeyDetail d = new KeyDetail();
    d.setStatus(Status.ACTIVE);
    when(repository.findByBucketAndKeyPathStartingWithAndStatus("bkt", "dir/", Status.ACTIVE))
        .thenReturn(List.of(d));

    List<KeyDetail> result = service.listActiveForBucketAndPath("bkt", "dir/");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getStatus()).isEqualTo(Status.ACTIVE);
  }

  @Test
  void updatePath_updatesKeyPaths() {
    KeyDetail d = new KeyDetail();
    d.setKeyPath("old/file.txt");
    when(repository.findByBucketAndKeyPathStartingWith("bkt", "old/")).thenReturn(List.of(d));

    service.updatePath("bkt", "old", "new");

    verify(repository).save(d);
    assertThat(d.getKeyPath()).isEqualTo("new/file.txt");
  }
}
