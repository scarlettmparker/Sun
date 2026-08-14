package com.sun.gaia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.model.ApiKeyEntity;
import com.sun.gaia.repository.ApiKeyRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

  @Mock
  private ApiKeyRepository repository;

  private ApiKeyService service;

  @BeforeEach
  void setUp() {
    service = new ApiKeyService(repository);
  }

  @Test
  void issueKey_storesHashedKeyAndReturnsPlaintextOnce() {
    when(repository.save(any(ApiKeyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

    ApiKeyService.ApiKeyIssue issue = service.issueKey(UUID.randomUUID(), "bot");

    assertThat(issue.plaintextKey()).startsWith("ns_").hasSize(43);
    assertThat(issue.apiKey().getKeyPrefix()).isEqualTo(issue.plaintextKey().substring(0, 12));
    assertThat(issue.apiKey().getKeyHash()).hasSize(64);
    assertThat(issue.apiKey().getKeyHash()).isNotEqualTo(issue.plaintextKey());
    assertThat(issue.apiKey().isEnabled()).isTrue();
  }

  @Test
  void issueKey_hashIsSha256OfPlaintext() {
    when(repository.save(any(ApiKeyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

    ApiKeyService.ApiKeyIssue issue = service.issueKey(UUID.randomUUID(), "bot");

    ArgumentCaptor<ApiKeyEntity> captor = ArgumentCaptor.forClass(ApiKeyEntity.class);
    verify(repository).save(captor.capture());
    String plaintext = issue.plaintextKey();
    String expectedHash = hex(sha256(plaintext));
    assertThat(captor.getValue().getKeyHash()).isEqualTo(expectedHash);
  }

  @Test
  void resolve_returnsKeyForMatchingPlaintext() {
    ApiKeyEntity entity = new ApiKeyEntity();
    entity.setId(UUID.randomUUID());
    when(repository.findByKeyHash(anyString())).thenReturn(Optional.of(entity));

    Optional<ApiKeyEntity> result = service.resolve("ns_whatever");

    assertThat(result).contains(entity);
  }

  @Test
  void resolve_hashesPlaintextBeforeLookup() {
    when(repository.findByKeyHash(anyString())).thenReturn(Optional.empty());

    service.resolve("ns_somekey");

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(repository).findByKeyHash(captor.capture());
    assertThat(captor.getValue()).isEqualTo(hex(sha256("ns_somekey")));
  }

  @Test
  void resolve_returnsEmptyWhenKeyUnknown() {
    when(repository.findByKeyHash(anyString())).thenReturn(Optional.empty());

    assertThat(service.resolve("ns_unknown")).isEmpty();
  }

  @Test
  void markUsed_recordsLastUsedAt() {
    ApiKeyEntity entity = new ApiKeyEntity();
    entity.setId(UUID.randomUUID());
    when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));
    when(repository.save(any(ApiKeyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.markUsed(entity.getId());

    assertThat(entity.getLastUsedAt()).isNotNull();
    verify(repository).save(entity);
  }

  @Test
  void revoke_disablesKey() {
    ApiKeyEntity entity = new ApiKeyEntity();
    entity.setId(UUID.randomUUID());
    entity.setEnabled(true);
    when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));
    when(repository.save(any(ApiKeyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.revoke(entity.getId());

    assertThat(entity.isEnabled()).isFalse();
    verify(repository).save(entity);
  }

  @Test
  void revoke_throwsWhenKeyNotFound() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.revoke(id))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("API key not found");
  }

  @Test
  void rotate_issuesNewPlaintext() {
    ApiKeyEntity entity = new ApiKeyEntity();
    entity.setId(UUID.randomUUID());
    entity.setKeyPrefix("ns_oldprefix");
    entity.setKeyHash("old-hash");
    entity.setLastUsedAt(LocalDateTime.now());
    when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));
    when(repository.save(any(ApiKeyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

    ApiKeyService.ApiKeyIssue issue = service.rotate(entity.getId());

    assertThat(issue.plaintextKey()).startsWith("ns_").hasSize(43);
    assertThat(entity.getKeyHash()).isNotEqualTo("old-hash");
    assertThat(entity.getKeyPrefix()).isNotEqualTo("ns_oldprefix");
    assertThat(entity.getLastUsedAt()).isNull();
    verify(repository).save(entity);
  }

  @Test
  void rotate_throwsWhenKeyNotFound() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.rotate(id))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("API key not found");
  }

  private static byte[] sha256(String value) {
    try {
      return MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private static String hex(byte[] bytes) {
    return HexFormat.of().formatHex(bytes);
  }
}
