package com.sun.medusa.repository;

import com.sun.medusa.model.PieceSpriteEntity;
import com.sun.base.repository.BaseRepository;
import java.util.Optional;
import java.util.UUID;

public interface PieceSpriteRepository extends BaseRepository<PieceSpriteEntity> {
  Optional<PieceSpriteEntity> findByPieceDefinitionId(UUID pieceDefinitionId);
}