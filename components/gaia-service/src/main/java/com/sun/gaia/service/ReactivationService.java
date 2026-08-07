package com.sun.gaia.service;

import com.sun.gaia.model.ReactivationTokenEntity;
import com.sun.gaia.repository.ReactivationTokenRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReactivationService {

  private final ReactivationTokenRepository tokenRepository;

  public ReactivationService(ReactivationTokenRepository tokenRepository) {
    this.tokenRepository = tokenRepository;
  }

  /**
   * Creates a 15-minute reactivation token for an account.
   *
   * @param accountId the account to reactivate
   * @return the saved token
   */
  public ReactivationTokenEntity createToken(UUID accountId) {
    ReactivationTokenEntity entity = new ReactivationTokenEntity();
    entity.setAccountId(accountId);
    entity.setToken(UUID.randomUUID().toString());
    entity.setExpiresAt(LocalDateTime.now().plusMinutes(15));
    entity.setUsed(false);
    return tokenRepository.save(entity);
  }

  /**
   * Marks a valid token used and returns its account.
   *
   * @param token the token to consume
   * @return the account id
   */
  public UUID useToken(String token) {
    ReactivationTokenEntity entity = tokenRepository.findByToken(token)
        .orElseThrow(() -> new IllegalArgumentException("Invalid reactivation token"));
    if (entity.isUsed()) {
      throw new IllegalStateException("Token already used");
    }
    if (entity.getExpiresAt() != null && entity.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new IllegalStateException("Token expired");
    }
    entity.setUsed(true);
    tokenRepository.save(entity);
    return entity.getAccountId();
  }
}
