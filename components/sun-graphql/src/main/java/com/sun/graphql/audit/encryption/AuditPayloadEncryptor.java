package com.sun.graphql.audit.encryption;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.graphql.audit.config.AuditProperties;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AES-GCM envelope encryption for sensitive audit payloads. Mints a per-row data
 * key wrapped by the configured KEK, and with no KEK falls back to redaction
 * only.
 */
@Component
public class AuditPayloadEncryptor {

  /**
   * Identifier recorded against each envelope, supports future key rotation.
   */
  public static final String KEK_ID = "kek-v1";

  private static final Logger logger = LoggerFactory.getLogger(AuditPayloadEncryptor.class);

  private static final int GCM_TAG_BITS = 128;
  private static final int IV_BYTES = 12;
  private static final int DEK_BITS = 256;

  private final ObjectMapper mapper;
  private final SecureRandom random = new SecureRandom();
  private final SecretKey kek;

  public AuditPayloadEncryptor(ObjectMapper mapper, AuditProperties properties) {
    this.mapper = mapper;
    this.kek = resolveKek(properties.encryption() == null ? null : properties.encryption().kek());
  }

  /**
   * Reports whether a usable KEK is configured.
   *
   * @return true when payload encryption is available
   */
  public boolean isEnabled() {
    return kek != null;
  }

  /**
   * Encrypts a plaintext payload into a JSON envelope.
   *
   * @param plaintext the value to protect
   * @return the envelope JSON, or null when encryption is disabled
   */
  public String encrypt(String plaintext) {
    if (kek == null) {
      return null;
    }
    try {
      byte[] dekBytes = new byte[DEK_BITS / 8];
      random.nextBytes(dekBytes);
      SecretKey dek = new SecretKeySpec(dekBytes, "AES");

      byte[] iv = new byte[IV_BYTES];
      random.nextBytes(iv);

      byte[] ciphertext = aesGcm(Cipher.ENCRYPT_MODE, dek, iv, plaintext.getBytes(StandardCharsets.UTF_8));
      byte[] wrappedDek = aesGcm(Cipher.ENCRYPT_MODE, kek, iv, dekBytes);

      ObjectNode envelope = mapper.createObjectNode();
      envelope.put("kekId", KEK_ID);
      envelope.put("iv", Base64.getEncoder().encodeToString(iv));
      envelope.put("wrappedDek", Base64.getEncoder().encodeToString(wrappedDek));
      envelope.put("ciphertext", Base64.getEncoder().encodeToString(ciphertext));
      return mapper.writeValueAsString(envelope);
    } catch (Exception e) {
      logger.error("Failed to encrypt audit payload; storing redacted payload only", e);
      return null;
    }
  }

  /**
   * Decrypts an envelope produced by {@link #encrypt(String)}.
   *
   * @param envelopeJson the stored envelope JSON
   * @return the recovered plaintext, or null when encryption is disabled
   */
  public String decrypt(String envelopeJson) {
    if (kek == null || envelopeJson == null) {
      return null;
    }
    try {
      JsonNode node = mapper.readTree(envelopeJson);
      byte[] iv = Base64.getDecoder().decode(node.get("iv").asText());
      byte[] wrappedDek = Base64.getDecoder().decode(node.get("wrappedDek").asText());
      byte[] ciphertext = Base64.getDecoder().decode(node.get("ciphertext").asText());

      byte[] dekBytes = aesGcm(Cipher.DECRYPT_MODE, kek, iv, wrappedDek);
      SecretKey dek = new SecretKeySpec(dekBytes, "AES");
      byte[] plain = aesGcm(Cipher.DECRYPT_MODE, dek, iv, ciphertext);
      return new String(plain, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to decrypt audit payload envelope", e);
    }
  }

  /**
   * Resolves the configured KEK.
   *
   * @param base64 the base64-encoded key, or null
   * @return the AES key, or null when unset or too short
   */
  private SecretKey resolveKek(String base64) {
    if (base64 == null || base64.isBlank()) {
      logger.warn("No audit KEK configured (audit.encryption.kek).");
      return null;
    }
    byte[] key = Base64.getDecoder().decode(base64);
    if (key.length < 32) {
      logger.warn("Configured audit KEK is shorter than 256 bits.");
      return null;
    }
    return new SecretKeySpec(key, 0, 32, "AES");
  }

  /**
   * Runs AES-GCM over the input.
   *
   * @param mode the cipher mode
   * @param key the AES key
   * @param iv the initialisation vector
   * @param input the bytes to process
   * @return the cipher output
   * @throws Exception when the cipher cannot run
   */
  private byte[] aesGcm(int mode, SecretKey key, byte[] iv, byte[] input) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(mode, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
    return cipher.doFinal(input);
  }

  /**
   * Generates a fresh 256-bit KEK as base64, to seed configuration.
   *
   * @return a base64-encoded 32-byte key
   * @throws Exception when the key generator is unavailable
   */
  public static String generateKek() throws Exception {
    KeyGenerator gen = KeyGenerator.getInstance("AES");
    gen.init(DEK_BITS);
    return Base64.getEncoder().encodeToString(gen.generateKey().getEncoded());
  }
}
