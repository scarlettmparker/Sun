package com.sun.gaia.model;

import com.sun.base.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "gaia_password_reset_tokens")
public class PasswordResetTokenEntity extends BaseEntity {

  @Column(name = "account_id", nullable = false)
  private UUID accountId;

  @Column(name = "token", nullable = false, unique = true)
  private String token;

  @Column(name = "expires_at")
  private LocalDateTime expiresAt;

  @Column(name = "used", nullable = false)
  private boolean used = false;

  public UUID getAccountId() {
    return accountId;
  }

  public void setAccountId(UUID accountId) {
    this.accountId = accountId;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public LocalDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(LocalDateTime expiresAt) {
    this.expiresAt = expiresAt;
  }

  public boolean isUsed() {
    return used;
  }

  public void setUsed(boolean used) {
    this.used = used;
  }
}
