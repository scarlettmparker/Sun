package com.sun.hades.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.hades.model.ReaderTextEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReaderTextRepository
    extends BaseRepository<ReaderTextEntity>, JpaSpecificationExecutor<ReaderTextEntity> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from ReaderTextEntity t where t.id = :id")
  Optional<ReaderTextEntity> findByIdForUpdate(@Param("id") UUID id);
}
