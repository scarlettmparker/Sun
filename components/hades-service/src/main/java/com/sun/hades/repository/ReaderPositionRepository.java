package com.sun.hades.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.hades.model.ReaderPositionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReaderPositionRepository extends BaseRepository<ReaderPositionEntity> {

  List<ReaderPositionEntity> findByTextId(UUID textId);

  @Query("select p from ReaderPositionEntity p where p.textId = :textId "
      + "and p.startOffset = :startOffset and p.endOffset = :endOffset")
  Optional<ReaderPositionEntity> findPosition(
      @Param("textId") UUID textId,
      @Param("startOffset") int startOffset,
      @Param("endOffset") int endOffset);
}
