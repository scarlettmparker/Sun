package com.sun.gaia.service;

import com.sun.gaia.model.PasswordResetTokenEntity;
import com.sun.gaia.repository.PasswordResetTokenRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PasswordResetService {

  private final PasswordResetTokenRepository tokenRepository;

  public PasswordResetService(PasswordResetTokenRepository tokenRepository) {
    this.tokenRepository = tokenRepository;
  }

  public PasswordResetTokenEntity createToken(UUID accountId) {
    PasswordResetTokenEntity entity = new PasswordResetTokenEntity();
    entity.setAccountId(accountId);
    entity.setToken(UUID.randomUUID().toString());
    entity.setExpiresAt(LocalDateTime.now().plusMinutes(15));
    entity.setUsed(false);
    return tokenRepository.save(entity);
  }

  public PasswordResetTokenEntity verifyToken(String token) {
    PasswordResetTokenEntity entity = tokenRepository.findByToken(token)
        .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));
    if (entity.isUsed()) {
      throw new IllegalStateException("Token already used");
    }
    if (entity.getExpiresAt() != null && entity.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new IllegalStateException("Token expired");
    }
    return entity;
  }

  public UUID useToken(String token) {
    PasswordResetTokenEntity entity = verifyToken(token);
    entity.setUsed(true);
    tokenRepository.save(entity);
    return entity.getAccountId();
  }
}
