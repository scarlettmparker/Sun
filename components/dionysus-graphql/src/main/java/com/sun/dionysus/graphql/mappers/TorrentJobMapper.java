package com.sun.dionysus.graphql.mappers;

import com.sun.dionysus.codegen.types.TorrentJob;
import com.sun.dionysus.model.TorrentJobEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Mapper for TorrentJobEntity to the GraphQL TorrentJob type.
 */
@Component
public class TorrentJobMapper {

  private final MagnetDetailMapper magnetDetailMapper;

  @Autowired
  public TorrentJobMapper(MagnetDetailMapper magnetDetailMapper) {
    this.magnetDetailMapper = magnetDetailMapper;
  }

  /**
   * Maps a TorrentJobEntity to its GraphQL representation, with metadata.
   */
  public TorrentJob map(TorrentJobEntity entity) {
    TorrentJob job =
        TorrentJob.newBuilder()
            .id(entity.getId().toString())
            .bucket(entity.getBucket())
            .targetKeyPath(entity.getTargetKeyPath())
            .sourceType(entity.getSourceType())
            .status(entity.getStatus().name())
            .infoHash(entity.getInfoHash())
            .totalBytes(entity.getTotalBytes())
            .downloadedBytes(entity.getDownloadedBytes())
            .uploadedBytes(entity.getUploadedBytes())
            .progress(entity.getProgress())
            .build();

    if (entity.getDownloadRateBps() != null) {
      job.setDownloadRateBps(entity.getDownloadRateBps());
    }
    if (entity.getUploadRateBps() != null) {
      job.setUploadRateBps(entity.getUploadRateBps());
    }
    if (entity.getPeersConnected() != null) {
      job.setPeersConnected(entity.getPeersConnected());
    }
    if (entity.getSeedsConnected() != null) {
      job.setSeedsConnected(entity.getSeedsConnected());
    }
    if (entity.getEtaSeconds() != null) {
      job.setEtaSeconds(entity.getEtaSeconds());
    }
    if (entity.getErrorMessage() != null) {
      job.setErrorMessage(entity.getErrorMessage());
    }
    if (entity.getCreatedAt() != null) {
      job.setCreatedAt(entity.getCreatedAt().toString());
    }
    if (entity.getCompletedAt() != null) {
      job.setCompletedAt(entity.getCompletedAt().toString());
    }
    if (entity.getMagnetDetail() != null) {
      job.setMagnetDetail(magnetDetailMapper.map(entity.getMagnetDetail()));
    }
    return job;
  }
}
