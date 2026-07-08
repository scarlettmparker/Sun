package com.sun.gaia.graphql.services;

import com.sun.fates.model.PersonEntity;
import com.sun.fates.service.PersonService;
import com.sun.gaia.codegen.types.Account;
import com.sun.gaia.codegen.types.AuthResult;
import com.sun.gaia.codegen.types.Configuration;
import com.sun.gaia.codegen.types.ConfigurationInput;
import com.sun.gaia.codegen.types.LoginInput;
import com.sun.gaia.codegen.types.PropertySetEntry;
import com.sun.gaia.codegen.types.PropertySetSchema;
import com.sun.gaia.codegen.types.PropertySetSchemaInput;
import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.codegen.types.QuerySuccess;
import com.sun.gaia.codegen.types.RegisterInput;
import com.sun.gaia.codegen.types.StandardError;
import com.sun.gaia.graphql.mappers.AccountMapper;
import com.sun.gaia.graphql.mappers.ConfigurationMapper;
import com.sun.gaia.graphql.mappers.PropertySetMapper;
import com.sun.gaia.model.AccountEntity;
import com.sun.gaia.model.PropertySetEntryEntity;
import com.sun.gaia.service.AccountService;
import com.sun.gaia.service.ConfigurationReconciler;
import com.sun.gaia.service.ConfigurationService;
import com.sun.gaia.service.EmailService;
import com.sun.gaia.service.JwtService;
import com.sun.gaia.service.PasswordResetService;
import com.sun.gaia.service.PropertySetService;
import com.sun.gaia.service.UserContextHolder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL business logic for accounts and authentication.
 */
@Service
public class GaiaGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(GaiaGraphQLService.class);

  private final AccountService accountService;
  private final PersonService personService;
  private final JwtService jwtService;
  private final EmailService emailService;
  private final PasswordResetService passwordResetService;
  private final AccountMapper accountMapper;
  private final PropertySetService propertySetService;
  private final ConfigurationService configurationService;
  private final ConfigurationReconciler configurationReconciler;
  private final PropertySetMapper propertySetMapper;
  private final ConfigurationMapper configurationMapper;
  private final String appBaseUrl;

  public GaiaGraphQLService(AccountService accountService, PersonService personService,
      JwtService jwtService, EmailService emailService,
      PasswordResetService passwordResetService, AccountMapper accountMapper,
      PropertySetService propertySetService, ConfigurationService configurationService,
      ConfigurationReconciler configurationReconciler, PropertySetMapper propertySetMapper,
      ConfigurationMapper configurationMapper,
      @Value("${app.base-url:http://localhost:5176}") String appBaseUrl) {
    this.accountService = accountService;
    this.personService = personService;
    this.jwtService = jwtService;
    this.emailService = emailService;
    this.passwordResetService = passwordResetService;
    this.accountMapper = accountMapper;
    this.propertySetService = propertySetService;
    this.configurationService = configurationService;
    this.configurationReconciler = configurationReconciler;
    this.propertySetMapper = propertySetMapper;
    this.configurationMapper = configurationMapper;
    this.appBaseUrl = appBaseUrl;
  }

  /**
   * Returns the currently authenticated account.
   *
   * @return the GraphQL Account, or null if not authenticated
   */
  @Transactional(readOnly = true)
  public Account me() {
    UUID userId = UserContextHolder.getUserId();
    if (userId == null) {
      return null;
    }
    return accountService.findById(userId)
        .map(accountMapper::map)
        .orElse(null);
  }

  /**
   * Locates an account by id.
   *
   * @param id the account id
   * @return the GraphQL Account, or null if not found
   */
  @Transactional(readOnly = true)
  public Account account(String id) {
    return accountService.findById(UUID.fromString(id))
        .map(accountMapper::map)
        .orElse(null);
  }

  /**
   * Lists all accounts.
   *
   * @return a list of GraphQL Account objects
   */
  @Transactional(readOnly = true)
  public List<Account> listAccounts() {
    return accountService.findAll().stream()
        .map(accountMapper::map)
        .collect(Collectors.toList());
  }

  /**
   * Registers a new account and person.
   *
   * @param input the registration input
   * @return the auth result with JWT token
   */
  @Transactional
  public AuthResult register(RegisterInput input) {
    if (accountService.findByUsername(input.getUsername()).isPresent()) {
      throw new IllegalArgumentException("Username already taken");
    }

    PersonEntity person = new PersonEntity();
    person.setFirstName(input.getFirstName());
    person.setLastName(input.getLastName());
    person.setDisplayName(input.getDisplayName());
    person.setEmail(input.getEmail());
    person = personService.save(person);

    AccountEntity account = accountService.createAccount(
        input.getUsername(), input.getPassword(), person.getId());

    String token = jwtService.generateToken(account.getId(), person.getId());
    logger.info("Registered account {} for person {}", account.getId(), person.getId());

    return AuthResult.newBuilder()
        .accountId(account.getId().toString())
        .personId(person.getId().toString())
        .token(token)
        .build();
  }

  /**
   * Authenticates an account.
   *
   * @param input the login input
   * @return the auth result with JWT token
   */
  public AuthResult login(LoginInput input) {
    AccountEntity account = accountService.findByUsername(input.getUsername())
        .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

    if (!accountService.verifyPassword(account, input.getPassword())) {
      throw new IllegalArgumentException("Invalid username or password");
    }

    String token = jwtService.generateToken(account.getId(), account.getPersonId());
    logger.info("Login succeeded for account {}", account.getId());

    return AuthResult.newBuilder()
        .accountId(account.getId().toString())
        .personId(account.getPersonId().toString())
        .token(token)
        .build();
  }

  /**
   * Logs out the current account.
   *
   * @return a QuerySuccess result
   */
  public QueryResult logout() {
    return QuerySuccess.newBuilder()
        .message("Logout succeeded")
        .build();
  }

  /**
   * Requests a password reset email.
   *
   * @param email the email to send the reset link to
   * @return a QuerySuccess result (always succeeds to prevent email enumeration)
   */
  @Transactional
  public QueryResult requestPasswordReset(String email) {
    return accountService.findByPersonEmail(email)
        .map(account -> {
          var token = passwordResetService.createToken(account.getId());
          String resetLink = appBaseUrl + "/reset-password?token=" + token.getToken();
          emailService.sendPasswordResetEmail(email, resetLink);
          return QuerySuccess.newBuilder()
              .message("Password reset email sent")
              .id(account.getId().toString())
              .build();
        })
        .orElseGet(() -> {
          logger.warn("Password reset requested for unknown email");
          return QuerySuccess.newBuilder()
              .message("Password reset email sent")
              .build();
        });
  }

  /**
   * Resets a password using a reset token.
   *
   * @param token the reset token
   * @param newPassword the new password
   * @return the result of the reset operation
   */
  @Transactional
  public QueryResult resetPassword(String token, String newPassword) {
    try {
      UUID accountId = passwordResetService.useToken(token);
      accountService.changePassword(accountId, newPassword);
      return QuerySuccess.newBuilder()
          .message("Password reset succeeded")
          .id(accountId.toString())
          .build();
    } catch (Exception e) {
      return StandardError.newBuilder()
          .message(e.getMessage())
          .build();
    }
  }

  /**
   * Changes the password for the currently authenticated account.
   *
   * @param currentPassword the current password
   * @param newPassword the new password
   * @return the result of the change operation
   */
  @Transactional
  public QueryResult changePassword(String currentPassword, String newPassword) {
    UUID userId = UserContextHolder.getUserId();
    if (userId == null) {
      return StandardError.newBuilder()
          .message("Not authenticated")
          .build();
    }

    AccountEntity account = accountService.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("Account not found"));

    if (!accountService.verifyPassword(account, currentPassword)) {
      return StandardError.newBuilder()
          .message("Current password incorrect")
          .build();
    }

    accountService.changePassword(userId, newPassword);
    return QuerySuccess.newBuilder()
        .message("Password changed")
        .id(userId.toString())
        .build();
  }

  /**
   * Returns one entry's values, or every active entry mapped by name.
   *
   * @param ownerKey the owner key
   * @param name the property set name
   * @param entry the entry name, or null for all entries
   * @return the values map, or null when a named entry is missing
   */
  @Transactional(readOnly = true)
  public Object propertySet(String ownerKey, String name, String entry) {
    if (entry == null) {
      Map<String, Object> all = new LinkedHashMap<>();
      for (PropertySetEntryEntity entity : propertySetService.listActiveEntries(ownerKey, name)) {
        all.put(entity.getEntryName(), entity.getValues());
      }
      return all;
    }
    return propertySetService.getEntry(ownerKey, name, entry)
        .map(PropertySetEntryEntity::getValues)
        .orElse(null);
  }

  /**
   * Lists all active entries in a property set.
   *
   * @param ownerKey the owner key
   * @param name the property set name
   * @return the entries
   */
  @Transactional(readOnly = true)
  public List<PropertySetEntry> propertySets(String ownerKey, String name) {
    return propertySetService.listActiveEntries(ownerKey, name).stream()
        .map(propertySetMapper::map)
        .collect(Collectors.toList());
  }

  /**
   * Locates the schema for a property set.
   *
   * @param ownerKey the owner key
   * @param name the property set name
   * @return the schema, or null when absent
   */
  @Transactional(readOnly = true)
  public PropertySetSchema propertySetSchema(String ownerKey, String name) {
    return propertySetService.getSchemaEntity(ownerKey, name)
        .map(propertySetMapper::map)
        .orElse(null);
  }

  /**
   * Lists all configurations.
   *
   * @return the configurations
   */
  @Transactional(readOnly = true)
  public List<Configuration> configurations() {
    return configurationService.list().stream()
        .map(configurationMapper::map)
        .collect(Collectors.toList());
  }

  /**
   * Locates a configuration by id.
   *
   * @param id the configuration id
   * @return the configuration, or null when absent
   */
  @Transactional(readOnly = true)
  public Configuration configuration(String id) {
    return configurationService.locate(UUID.fromString(id))
        .map(configurationMapper::map)
        .orElse(null);
  }

  /**
   * Creates or replaces a property-set entry.
   *
   * @param ownerKey the owner key
   * @param name the property set name
   * @param entry the entry name
   * @param values the values to store
   * @return the saved entry
   */
  @Transactional
  public PropertySetEntry upsertPropertyEntry(String ownerKey, String name, String entry,
      Object values) {
    return propertySetMapper.map(
        propertySetService.upsertEntry(ownerKey, name, entry, asMap(values), false));
  }

  /**
   * Sets a single property on an entry.
   *
   * @param ownerKey the owner key
   * @param name the property set name
   * @param entry the entry name
   * @param property the property name
   * @param value the property value
   * @return the saved entry
   */
  @Transactional
  public PropertySetEntry setProperty(String ownerKey, String name, String entry, String property,
      Object value) {
    return propertySetMapper.map(
        propertySetService.setProperty(ownerKey, name, entry, property, value));
  }

  /**
   * Registers a property-set schema.
   *
   * @param input the schema input
   * @return the saved schema
   */
  @Transactional
  public PropertySetSchema registerPropertySetSchema(PropertySetSchemaInput input) {
    return propertySetMapper.map(propertySetService.upsertSchema(
        input.getOwnerKey(), input.getName(),
        input.getConfigurable() != null && input.getConfigurable(),
        asMap(input.getProperties())));
  }

  /**
   * Creates a configuration.
   *
   * @param input the configuration input
   * @return the saved configuration
   */
  @Transactional
  public Configuration createConfiguration(ConfigurationInput input) {
    return configurationMapper.map(configurationService.create(
        input.getName(), input.getDescription(),
        input.getEnabled() == null || input.getEnabled(), asMap(input.getContent())));
  }

  /**
   * Updates a configuration.
   *
   * @param id the configuration id
   * @param input the configuration input
   * @return the saved configuration
   */
  @Transactional
  public Configuration updateConfiguration(String id, ConfigurationInput input) {
    return configurationMapper.map(configurationService.update(
        UUID.fromString(id), input.getName(), input.getDescription(),
        input.getEnabled() == null || input.getEnabled(), asMap(input.getContent())));
  }

  /**
   * Deletes a configuration.
   *
   * @param id the configuration id
   * @return a success result
   */
  @Transactional
  public QueryResult deleteConfiguration(String id) {
    configurationService.deleteById(UUID.fromString(id));
    return QuerySuccess.newBuilder()
        .message("Configuration deleted")
        .id(id)
        .build();
  }

  /**
   * Applies a configuration's desired state immediately.
   *
   * @param id the configuration id
   * @return the reconciled configuration
   */
  @Transactional
  public Configuration applyConfiguration(String id) {
    return configurationMapper.map(configurationReconciler.reconcileById(UUID.fromString(id)));
  }

  /**
   * Coerces a JSON input value into a string-keyed map.
   *
   * @param value the JSON value
   * @return the coerced map
   */
  private Map<String, Object> asMap(Object value) {
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> result = new LinkedHashMap<>();
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        result.put(String.valueOf(entry.getKey()), entry.getValue());
      }
      return result;
    }
    throw new IllegalArgumentException("Expected a JSON object");
  }
}
