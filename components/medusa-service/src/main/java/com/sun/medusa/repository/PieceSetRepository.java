package com.sun.medusa.repository;

import com.sun.medusa.model.PieceSetEntity;
import com.sun.base.repository.BaseRepository;
import java.util.List;
import java.util.UUID;

public interface PieceSetRepository extends BaseRepository<PieceSetEntity> {
  List<PieceSetEntity> findBySystemTrueOrCreatedBy(UUID createdBy);
}