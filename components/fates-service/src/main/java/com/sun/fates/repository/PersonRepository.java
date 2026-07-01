package com.sun.fates.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.fates.model.PersonEntity;
import java.util.Optional;
import java.util.UUID;

public interface PersonRepository extends BaseRepository<PersonEntity> {

  Optional<PersonEntity> findByEmail(String email);
}
