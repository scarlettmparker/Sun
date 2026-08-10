package com.sun.gaia.service;

import com.sun.base.service.BaseService;
import com.sun.fates.model.PersonEntity;
import com.sun.fates.service.PersonService;
import com.sun.gaia.model.AccountEntity;
import com.sun.gaia.model.enums.AccountStatus;
import com.sun.gaia.model.enums.AccountType;
import com.sun.gaia.repository.AccountRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AccountService extends BaseService<AccountEntity> {

  private static final UUID NIL_PERSON_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  private final AccountRepository accountRepository;
  private final PersonService personService;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  public AccountService(AccountRepository repository, PersonService personService) {
    super(repository);
    this.accountRepository = repository;
    this.personService = personService;
  }

  public Optional<AccountEntity> findByUsername(String username) {
    return accountRepository.findByUsername(username);
  }

  public Optional<AccountEntity> findById(UUID id) {
    return accountRepository.findById(id);
  }

  public AccountEntity createAccount(String username, String rawPassword, UUID personId) {
    AccountEntity entity = new AccountEntity();
    entity.setUsername(username);
    entity.setPasswordHash(passwordEncoder.encode(rawPassword));
    entity.setPersonId(personId);
    entity.setStatus(AccountStatus.ACTIVE);
    entity.setProvider("local");
    return save(entity);
  }

  public boolean verifyPassword(AccountEntity account, String rawPassword) {
    return passwordEncoder.matches(rawPassword, account.getPasswordHash());
  }

  public void changePassword(UUID accountId, String newPassword) {
    AccountEntity account = findById(accountId)
        .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
    account.setPasswordHash(passwordEncoder.encode(newPassword));
    save(account);
  }

  /**
   * Returns every account linked to a person with the given email.
   *
   * @param email the person email
   * @return the accounts, or empty when no person matches
   */
  public List<AccountEntity> findByPersonEmail(String email) {
    return personService.findByEmail(email)
        .map(person -> accountRepository.findAllByPersonId(person.getId()))
        .orElseGet(List::of);
  }

  /**
   * Finds or creates a non-login ghost account for the given owner key.
   *
   * @param key the owner key, used as the username
   * @return the ghost account
   */
  public AccountEntity upsertGhostAccount(String key) {
    return accountRepository.findByUsernameAndAccountType(key, AccountType.GHOST)
        .orElseGet(() -> {
          AccountEntity account = new AccountEntity();
          account.setUsername(key);
          account.setPasswordHash("!");
          account.setPersonId(NIL_PERSON_ID);
          account.setStatus(AccountStatus.ACTIVE);
          account.setProvider("ghost");
          account.setAccountType(AccountType.GHOST);
          return save(account);
        });
  }

  /**
   * Marks an account deactivated by its owner, revoking all sessions.
   */
  public AccountEntity deactivateAccount(UUID accountId) {
    AccountEntity account = findById(accountId)
        .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
    account.setStatus(AccountStatus.DEACTIVATED);
    return save(account);
  }

  /**
   * Finds or creates an account for an OAuth provider identity, re-syncing the
   * person's display name and email whenever they differ.
   *
   * @param provider the provider key (e.g. "discord")
   * @param providerId the provider's user id
   * @param username the provider username
   * @param displayName the provider display name, or null when absent
   * @param email the provider-reported email, or null when absent
   * @return the account, with the person identity synced
   */
  public AccountEntity upsertProviderAccount(
      String provider, String providerId, String username, String displayName, String email) {
    Optional<AccountEntity> existing =
        accountRepository.findByProviderAndProviderId(provider, providerId);
    if (existing.isPresent()) {
      AccountEntity account = existing.get();
      syncPerson(account, username, displayName, email);
      return account;
    }
    PersonEntity person = new PersonEntity();
    person.setFirstName(username);
    person.setLastName("");
    person.setDisplayName(displayName == null || displayName.isBlank() ? username : displayName);
    person.setEmail(email);
    person = personService.save(person);

    AccountEntity account = new AccountEntity();
    account.setUsername(provider + "_" + providerId);
    account.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
    account.setPersonId(person.getId());
    account.setStatus(AccountStatus.ACTIVE);
    account.setProvider(provider);
    account.setProviderId(providerId);
    return save(account);
  }

  /**
   * Writes the provider's identity onto the account's person when it differs.
   */
  private void syncPerson(AccountEntity account, String username, String displayName,
      String email) {
    personService.findById(account.getPersonId()).ifPresent(person -> {
      boolean changed = false;
      if (username != null && !username.equals(person.getFirstName())) {
        person.setFirstName(username);
        changed = true;
      }
      String resolvedDisplay =
          displayName == null || displayName.isBlank() ? username : displayName;
      if (resolvedDisplay != null && !resolvedDisplay.equals(person.getDisplayName())) {
        person.setDisplayName(resolvedDisplay);
        changed = true;
      }
      if (email != null && !email.isBlank() && !email.equals(person.getEmail())) {
        person.setEmail(email);
        changed = true;
      }
      if (changed) {
        personService.save(person);
      }
    });
  }
}
