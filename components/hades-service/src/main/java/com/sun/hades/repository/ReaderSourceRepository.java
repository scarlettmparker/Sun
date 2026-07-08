package com.sun.hades.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.hades.model.ReaderSourceEntity;
import java.util.Optional;

public interface ReaderSourceRepository extends BaseRepository<ReaderSourceEntity> {

  Optional<ReaderSourceEntity> findByUrl(String url);
}
