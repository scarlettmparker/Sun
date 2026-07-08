package com.sun.hades.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.hades.model.ReaderPositionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReaderPositionRepository extends BaseRepository<ReaderPositionEntity> {

  List<ReaderPositionEntity> findByTextId(UUID textId);

  Optional<ReaderPositionEntity> findByTextIdAndStartOffsetAndEndOffset(
      UUID textId, int startOffset, int endOffset);
}
