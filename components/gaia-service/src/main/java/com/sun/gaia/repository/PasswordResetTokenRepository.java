package com.sun.gaia.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.gaia.model.PasswordResetTokenEntity;
import java.util.Optional;

public interface PasswordResetTokenRepository extends BaseRepository<PasswordResetTokenEntity> {

  Optional<PasswordResetTokenEntity> findByToken(String token);
}
