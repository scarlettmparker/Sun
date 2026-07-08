package com.sun.hades.model;

import com.sun.base.model.BaseEntity;
import com.sun.hades.model.enums.CefrLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Reader-specific profile for a member, synced from Discord on login.
 */
@Entity
@Table(name = "reader_accounts")
public class ReaderAccountEntity extends BaseEntity {

  @Column(name = "gaia_account_id", nullable = false, unique = true)
  private UUID gaiaAccountId;

  @Column(name = "discord_id", nullable = false, unique = true)
  private String discordId;

  @Column(name = "discord_username")
  private String discordUsername;

  @Column(name = "global_name")
  private String globalName;

  @Column(name = "avatar")
  private String avatar;

  @Enumerated(EnumType.STRING)
  @Column(name = "cefr_level")
  private CefrLevel cefrLevel;

  public UUID getGaiaAccountId() {
    return gaiaAccountId;
  }

  public void setGaiaAccountId(UUID gaiaAccountId) {
    this.gaiaAccountId = gaiaAccountId;
  }

  public String getDiscordId() {
    return discordId;
  }

  public void setDiscordId(String discordId) {
    this.discordId = discordId;
  }

  public String getDiscordUsername() {
    return discordUsername;
  }

  public void setDiscordUsername(String discordUsername) {
    this.discordUsername = discordUsername;
  }

  public String getGlobalName() {
    return globalName;
  }

  public void setGlobalName(String globalName) {
    this.globalName = globalName;
  }

  public String getAvatar() {
    return avatar;
  }

  public void setAvatar(String avatar) {
    this.avatar = avatar;
  }

  public CefrLevel getCefrLevel() {
    return cefrLevel;
  }

  public void setCefrLevel(CefrLevel cefrLevel) {
    this.cefrLevel = cefrLevel;
  }
}
