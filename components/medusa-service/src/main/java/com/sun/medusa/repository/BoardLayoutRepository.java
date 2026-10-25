package com.sun.medusa.repository;

import com.sun.medusa.model.BoardLayoutEntity;
import com.sun.base.repository.BaseRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BoardLayoutRepository extends BaseRepository<BoardLayoutEntity> {
  Optional<BoardLayoutEntity> findBySlug(String slug);

  List<BoardLayoutEntity> findBySystemTrueOrCreatedByIsNullOrCreatedBy(UUID createdBy);
}