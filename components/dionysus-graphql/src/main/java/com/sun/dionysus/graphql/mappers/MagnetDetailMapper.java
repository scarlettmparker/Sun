package com.sun.dionysus.graphql.mappers;

import com.sun.dionysus.codegen.types.MagnetDetail;
import com.sun.dionysus.codegen.types.TorrentFile;
import com.sun.dionysus.model.MagnetDetailEntity;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Mapper for MagnetDetailEntity to the GraphQL MagnetDetail type.
 */
@Component
public class MagnetDetailMapper {

  /**
   * Maps a MagnetDetailEntity to its GraphQL representation, including files.
   */
  public MagnetDetail map(MagnetDetailEntity entity) {
    MagnetDetail detail =
        MagnetDetail.newBuilder()
            .id(entity.getId().toString())
            .infoHash(entity.getInfoHash())
            .displayName(entity.getDisplayName())
            .totalSize(entity.getTotalSize())
            .isPrivate(entity.isPrivate())
            .build();

    if (entity.getInfoHashVersion() != null) {
      detail.setInfoHashVersion(entity.getInfoHashVersion());
    }
    if (entity.getSourceUri() != null) {
      detail.setSourceUri(entity.getSourceUri());
    }
    if (entity.getComment() != null) {
      detail.setComment(entity.getComment());
    }
    if (entity.getCreatedByTorrent() != null) {
      detail.setCreatedBy(entity.getCreatedByTorrent());
    }
    if (entity.getPieceLength() != null) {
      detail.setPieceLength(entity.getPieceLength());
    }
    if (entity.getPieceCount() != null) {
      detail.setPieceCount(entity.getPieceCount());
    }
    if (entity.getTrackers() != null && !entity.getTrackers().isEmpty()) {
      detail.setTrackers(entity.getTrackers());
    }
    if (entity.getFiles() != null && !entity.getFiles().isEmpty()) {
      List<TorrentFile> files =
          entity.getFiles().stream()
              .map(
                  f ->
                      TorrentFile.newBuilder()
                          .indexInTorrent(f.getIndexInTorrent())
                          .path(f.getPath())
                          .size(f.getSize())
                          .build())
              .toList();
      detail.setFiles(files);
    }
    return detail;
  }
}
