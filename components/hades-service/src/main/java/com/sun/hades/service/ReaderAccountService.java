package com.sun.hades.service;

import com.sun.base.service.BaseService;
import com.sun.hades.model.ReaderAccountEntity;
import com.sun.hades.model.enums.CefrLevel;
import com.sun.hades.repository.ReaderAccountRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for reader-member profiles synced from Discord.
 */
@Service
@Transactional
public class ReaderAccountService extends BaseService<ReaderAccountEntity> {

  private final ReaderAccountRepository accountRepository;

  public ReaderAccountService(ReaderAccountRepository repository) {
    super(repository);
    this.accountRepository = repository;
  }

  /**
   * Finds a reader account by its gaia account id.
   *
   * @param gaiaAccountId the gaia account id
   * @return the reader account, or empty
   */
  public Optional<ReaderAccountEntity> findByGaiaAccountId(UUID gaiaAccountId) {
    return accountRepository.findByGaiaAccountId(gaiaAccountId);
  }

  /**
   * Creates or updates the reader account for a Discord identity.
   *
   * @param gaiaAccountId the gaia account id linking the identity
   * @param discordId the Discord user id
   * @param username the Discord username
   * @param globalName the Discord global display name
   * @param avatar the Discord avatar hash
   * @param cefrLevel the CEFR level derived from guild roles
   * @return the reader account id
   */
  public UUID upsertFromDiscord(UUID gaiaAccountId, String discordId, String username,
      String globalName, String avatar, CefrLevel cefrLevel) {
    ReaderAccountEntity account = accountRepository.findByDiscordId(discordId)
        .orElseGet(ReaderAccountEntity::new);
    account.setGaiaAccountId(gaiaAccountId);
    account.setDiscordId(discordId);
    account.setDiscordUsername(username);
    account.setGlobalName(globalName);
    account.setAvatar(avatar);
    account.setCefrLevel(cefrLevel);
    return accountRepository.save(account).getId();
  }
}
