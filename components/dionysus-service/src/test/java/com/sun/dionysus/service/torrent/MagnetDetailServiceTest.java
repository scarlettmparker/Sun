package com.sun.dionysus.service.torrent;

import com.sun.dionysus.model.MagnetDetailEntity;
import com.sun.dionysus.repository.MagnetDetailEntityRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MagnetDetailService.
 */
@ExtendWith(MockitoExtension.class)
class MagnetDetailServiceTest {

  @Mock
  private MagnetDetailEntityRepository repository;

  @InjectMocks
  private MagnetDetailService service;

  @Test
  void findByInfoHash_delegates() {
    MagnetDetailEntity magnet = new MagnetDetailEntity();
    magnet.setInfoHash("abc123");
    when(repository.findByInfoHash("abc123")).thenReturn(Optional.of(magnet));

    Optional<MagnetDetailEntity> result = service.findByInfoHash("abc123");

    assertThat(result).contains(magnet);
  }
}
