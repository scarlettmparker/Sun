package com.sun.gaia.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final SecretKey key;
  private final long expirationMs;

  public JwtService(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes());
    this.expirationMs = expirationMs;
  }

  public String generateToken(UUID accountId, UUID personId) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + expirationMs);
    return Jwts.builder()
        .subject(accountId.toString())
        .claim("pid", personId.toString())
        .issuedAt(now)
        .expiration(expiry)
        .signWith(key)
        .compact();
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

  public boolean isValid(String token) {
    try {
      verifyToken(token);
      return true;
    } catch (JwtException e) {
      return false;
    }
  }
}
