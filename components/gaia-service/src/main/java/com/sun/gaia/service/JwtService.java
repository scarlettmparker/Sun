package com.sun.gaia.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

  private final SecretKey key;
  private final long expirationMs;
  private final StringRedisTemplate redisTemplate;

  public JwtService(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.expiration-ms:86400000}") long expirationMs,
      ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes());
    this.expirationMs = expirationMs;
    this.redisTemplate = redisTemplateProvider.getIfAvailable();
  }

  public String generateToken(UUID accountId, UUID personId) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + expirationMs);
    String jti = UUID.randomUUID().toString();
    String token = Jwts.builder()
        .subject(accountId.toString())
        .claim("pid", personId.toString())
        .claim("jti", jti)
        .issuedAt(now)
        .expiration(expiry)
        .signWith(key)
        .compact();
    if (redisTemplate != null) {
      try {
        redisTemplate.opsForValue().set("session:" + jti, accountId.toString(), expirationMs, TimeUnit.MILLISECONDS);
        redisTemplate.opsForValue().set("session:account:" + accountId, jti, expirationMs, TimeUnit.MILLISECONDS);
      } catch (Exception e) {
        logger.warn("Failed to persist session {} to Redis", jti, e);
      }
    }
    return token;
  }

  public Claims verifyToken(String token) {
    return Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public UUID extractAccountId(String token) {
    Claims claims = verifyToken(token);
    return UUID.fromString(claims.getSubject());
  }

  public UUID extractPersonId(String token) {
    Claims claims = verifyToken(token);
    return UUID.fromString(claims.get("pid", String.class));
  }

  public String extractJti(String token) {
    Claims claims = verifyToken(token);
    return claims.get("jti", String.class);
  }

  public boolean isValid(String token) {
    try {
      Claims claims = verifyToken(token);
      String jti = claims.get("jti", String.class);
      if (jti != null && redisTemplate != null) {
        try {
          Boolean revoked = redisTemplate.hasKey("revoked:" + jti);
          if (Boolean.TRUE.equals(revoked)) {
            return false;
          }
        } catch (Exception e) {
          logger.warn("Redis check failed for jti {}", jti, e);
        }
      }
      return true;
    } catch (JwtException e) {
      return false;
    }
  }

  /**
   * Revokes a token by its jti until natural expiry.
   */
  public void revokeToken(String token) {
    if (redisTemplate == null) {
      return;
    }
    try {
      Claims claims = verifyToken(token);
      String jti = claims.get("jti", String.class);
      if (jti == null) {
        return;
      }
      Date expiry = claims.getExpiration();
      long ttlMs = expiry != null ? expiry.getTime() - System.currentTimeMillis() : expirationMs;
      if (ttlMs <= 0) {
        ttlMs = expirationMs;
      }
      redisTemplate.opsForValue().set("revoked:" + jti, "1", ttlMs, TimeUnit.MILLISECONDS);
      redisTemplate.delete("session:" + jti);
      String accountId = claims.getSubject();
      if (accountId != null) {
        redisTemplate.delete("session:account:" + accountId);
      }
    } catch (Exception e) {
      logger.warn("Failed to revoke token", e);
    }
  }

  /**
   * Revokes all sessions for an account.
   */
  public void revokeAllForAccount(UUID accountId) {
    if (redisTemplate == null || accountId == null) {
      return;
    }
    try {
      redisTemplate.delete("session:account:" + accountId);
      Set<String> keys = redisTemplate.keys("session:*");
      if (keys != null) {
        for (String key : keys) {
          if (key.startsWith("session:account:")) {
            continue;
          }
          try {
            String val = redisTemplate.opsForValue().get(key);
            if (accountId.toString().equals(val)) {
              String jti = key.substring("session:".length());
              Long ttl = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
              long ttlMs = ttl != null && ttl > 0 ? ttl : expirationMs;
              redisTemplate.opsForValue().set("revoked:" + jti, "1", ttlMs, TimeUnit.MILLISECONDS);
              redisTemplate.delete(key);
            }
          } catch (Exception e) {
            logger.warn("Failed to revoke session key {}", key, e);
          }
        }
      }
      redisTemplate.opsForValue().set("revoked:account:" + accountId, "1", expirationMs, TimeUnit.MILLISECONDS);
    } catch (Exception e) {
      logger.warn("Failed to revoke all for account {}", accountId, e);
    }
  }
}
