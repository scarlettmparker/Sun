package com.sun.medusa.repository;

import com.sun.medusa.model.PieceDefinitionEntity;
import com.sun.base.repository.BaseRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PieceDefinitionRepository extends BaseRepository<PieceDefinitionEntity> {
  Optional<PieceDefinitionEntity> findBySlug(String slug);

  List<PieceDefinitionEntity> findBySystemTrueOrCreatedBy(UUID createdBy);
}