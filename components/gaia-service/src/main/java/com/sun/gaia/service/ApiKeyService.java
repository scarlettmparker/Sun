package com.sun.gaia.service;

import com.sun.gaia.model.ApiKeyEntity;
import com.sun.gaia.repository.ApiKeyRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues and resolves API keys for service accounts.
 */
@Service
public class ApiKeyService {

  private static final String KEY_PREFIX = "ns_";
  private static final int KEY_HEX_LENGTH = 40;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final ApiKeyRepository repository;

  public ApiKeyService(ApiKeyRepository repository) {
    this.repository = repository;
  }

  /**
   * Creates a new key for the account and returns its one-time plaintext.
   *
   * @param accountId the owning service account
   * @param name      the key label
   * @return the saved key and its one-time plaintext
   */
  @Transactional
  public ApiKeyIssue issueKey(UUID accountId, String name) {
    String plaintext = generateKey();
    ApiKeyEntity entity = new ApiKeyEntity();
    entity.setAccountId(accountId);
    entity.setName(name);
    entity.setKeyPrefix(plaintext.substring(0, 12));
    entity.setKeyHash(hash(plaintext));
    return new ApiKeyIssue(repository.save(entity), plaintext);
  }

  /**
   * Returns the key that matches the plaintext, if any.
   *
   * @param plaintext the submitted key value
   * @return the matching key, or empty when unknown
   */
  @Transactional(readOnly = true)
  public Optional<ApiKeyEntity> resolve(String plaintext) {
    return repository.findByKeyHash(hash(plaintext));
  }

  /**
   * Records the time the key was last used.
   *
   * @param id the key id
   */
  @Transactional
  public void markUsed(UUID id) {
    repository.findById(id).ifPresent(key -> {
      key.setLastUsedAt(LocalDateTime.now());
      repository.save(key);
    });
  }

  /**
   * Disables the key.
   *
   * @param id the key id
   */
  @Transactional
  public void revoke(UUID id) {
    ApiKeyEntity key = repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("API key not found: " + id));
    key.setEnabled(false);
    repository.save(key);
  }

  /**
   * Issues a fresh plaintext for an existing key.
   *
   * @param id the key id
   * @return the updated key and its one-time plaintext
   */
  @Transactional
  public ApiKeyIssue rotate(UUID id) {
    ApiKeyEntity key = repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("API key not found: " + id));
    String plaintext = generateKey();
    key.setKeyPrefix(plaintext.substring(0, 12));
    key.setKeyHash(hash(plaintext));
    key.setLastUsedAt(null);
    return new ApiKeyIssue(repository.save(key), plaintext);
  }

  /**
   * A newly issued key together with its one-time plaintext.
   */
  public record ApiKeyIssue(ApiKeyEntity apiKey, String plaintextKey) {}

  private String generateKey() {
    byte[] bytes = new byte[KEY_HEX_LENGTH / 2];
    RANDOM.nextBytes(bytes);
    return KEY_PREFIX + HexFormat.of().formatHex(bytes);
  }

  private String hash(String plaintext) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(plaintext.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to hash API key", e);
    }
  }
}
