package com.sun.gaia.service;

import com.sun.gaia.model.MagicLinkTokenEntity;
import com.sun.gaia.repository.MagicLinkTokenRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MagicLinkService {

  private final MagicLinkTokenRepository tokenRepository;

  public MagicLinkService(MagicLinkTokenRepository tokenRepository) {
    this.tokenRepository = tokenRepository;
  }

  public MagicLinkTokenEntity createToken(UUID accountId) {
    tokenRepository.deleteByAccountId(accountId);
    MagicLinkTokenEntity entity = new MagicLinkTokenEntity();
    entity.setAccountId(accountId);
    entity.setToken(UUID.randomUUID().toString());
    entity.setExpiresAt(LocalDateTime.now().plusMinutes(15));
    entity.setUsed(false);
    return tokenRepository.save(entity);
  }

  public UUID useToken(String token) {
    MagicLinkTokenEntity entity = tokenRepository.findByToken(token)
        .orElseThrow(() -> new IllegalArgumentException("Invalid magic link token"));
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