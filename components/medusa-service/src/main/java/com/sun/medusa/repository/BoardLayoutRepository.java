package com.sun.medusa.repository;

import com.sun.medusa.model.BoardLayoutEntity;
import com.sun.base.repository.BaseRepository;
import java.util.Optional;

public interface BoardLayoutRepository extends BaseRepository<BoardLayoutEntity> {
  Optional<BoardLayoutEntity> findBySlug(String slug);
}