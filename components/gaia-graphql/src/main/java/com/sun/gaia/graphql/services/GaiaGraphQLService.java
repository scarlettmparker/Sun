package com.sun.gaia.graphql.services;

import com.sun.fates.model.PersonEntity;
import com.sun.fates.service.PersonService;
import com.sun.gaia.codegen.types.Account;
import com.sun.gaia.codegen.types.ApiKey;
import com.sun.gaia.codegen.types.AuthResult;
import com.sun.gaia.codegen.types.Configuration;
import com.sun.gaia.codegen.types.ConfigurationInput;
import com.sun.gaia.codegen.types.HubApp;
import com.sun.gaia.codegen.types.HubAppInput;
import com.sun.gaia.codegen.types.HubMode;
import com.sun.gaia.codegen.types.HubRegistry;
import com.sun.gaia.codegen.types.HubRegistryInput;
import com.sun.gaia.codegen.types.IpWhitelistEntry;
import com.sun.gaia.codegen.types.IpWhitelistEntryInput;
import com.sun.gaia.codegen.types.IssuedApiKey;
import com.sun.gaia.codegen.types.TailscaleDevice;
import com.sun.gaia.codegen.types.LoginInput;
import com.sun.gaia.codegen.types.PagedAccounts;
import com.sun.gaia.codegen.types.PageInfo;
import com.sun.gaia.codegen.types.PaginationInput;
import com.sun.gaia.codegen.types.PropertySetEntry;
import com.sun.gaia.codegen.types.PropertySetSchema;
import com.sun.gaia.codegen.types.PropertySetSchemaInput;
import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.codegen.types.QuerySuccess;
import com.sun.gaia.codegen.types.RegisterInput;
import com.sun.gaia.codegen.types.StandardError;
import com.sun.gaia.graphql.mappers.AccountMapper;
import com.sun.gaia.graphql.mappers.ApiKeyMapper;
import com.sun.gaia.graphql.mappers.ConfigurationMapper;
import com.sun.gaia.graphql.mappers.PropertySetMapper;
import com.sun.gaia.model.AccountEntity;
import com.sun.gaia.model.IpWhitelistEntryEntity;
import com.sun.gaia.model.PropertySetEntryEntity;
import com.sun.gaia.codegen.types.RemoteUserType;
import com.sun.gaia.model.enums.AccountStatus;
import com.sun.gaia.repository.AccountRepository;
import com.sun.gaia.service.AccountService;
import com.sun.gaia.service.ApiKeyService;
import com.sun.gaia.graphql.mappers.IpWhitelistMapper;
import com.sun.gaia.graphql.mappers.TailscaleDeviceMapper;
import com.sun.gaia.service.ConfigurationReconciler;
import com.sun.base.util.FilterBuilder;
import com.sun.base.util.FilterSpec;
import com.sun.base.util.GraphQLSupport;
import com.sun.base.util.PageRequests;
import com.sun.gaia.service.ConfigurationService;
import com.sun.gaia.service.EmailService;
import com.sun.gaia.service.IpWhitelistService;
import com.sun.gaia.service.TailscaleDeviceService;
import com.sun.gaia.service.JwtService;
import com.sun.gaia.service.PasswordResetService;
import com.sun.gaia.service.ReactivationService;
import com.sun.gaia.service.PropertySetService;
import com.sun.gaia.service.UserContextHolder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * GraphQL business logic for accounts and authentication.
 */
@Service
public class GaiaGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(GaiaGraphQLService.class);

  /**
   * Property set backing the hub registry.
   */
  private static final String HUB_OWNER_KEY = "hub";
  private static final String HUB_SET_NAME = "registry";
  private static final String HUB_ENTRY_NAME = "apps";

  private final AccountService accountService;
  private final ApiKeyService apiKeyService;
  private final AccountRepository accountRepository;
  private final PersonService personService;
  private final JwtService jwtService;
  private final EmailService emailService;
  private final PasswordResetService passwordResetService;
  private final ReactivationService reactivationService;
  private final AccountMapper accountMapper;
  private final ApiKeyMapper apiKeyMapper;
  private final PropertySetService propertySetService;
  private final ConfigurationService configurationService;
  private final ConfigurationReconciler configurationReconciler;
  private final PropertySetMapper propertySetMapper;
  private final ConfigurationMapper configurationMapper;
  private final IpWhitelistService ipWhitelistService;
  private final IpWhitelistMapper ipWhitelistMapper;
  private final TailscaleDeviceService tailscaleDeviceService;
  private final TailscaleDeviceMapper tailscaleDeviceMapper;
  private final String appBaseUrl;

  public GaiaGraphQLService(
    AccountService accountService,
    ApiKeyService apiKeyService,
    AccountRepository accountRepository,
    PersonService personService,
    JwtService jwtService,
    EmailService emailService,
    PasswordResetService passwordResetService,
    ReactivationService reactivationService,
    AccountMapper accountMapper,
    ApiKeyMapper apiKeyMapper,
    PropertySetService propertySetService,
    ConfigurationService configurationService,
    ConfigurationReconciler configurationReconciler,
    PropertySetMapper propertySetMapper,
    ConfigurationMapper configurationMapper,
    IpWhitelistService ipWhitelistService,
    IpWhitelistMapper ipWhitelistMapper,
    TailscaleDeviceService tailscaleDeviceService,
    TailscaleDeviceMapper tailscaleDeviceMapper,
    @Value("${app.base-url}") String appBaseUrl
  ) {
    this.accountService = accountService;
    this.apiKeyService = apiKeyService;
    this.accountRepository = accountRepository;
    this.personService = personService;
    this.jwtService = jwtService;
    this.emailService = emailService;
    this.passwordResetService = passwordResetService;
    this.reactivationService = reactivationService;
    this.accountMapper = accountMapper;
    this.apiKeyMapper = apiKeyMapper;
    this.propertySetService = propertySetService;
    this.configurationService = configurationService;
    this.configurationReconciler = configurationReconciler;
    this.propertySetMapper = propertySetMapper;
    this.configurationMapper = configurationMapper;
    this.ipWhitelistService = ipWhitelistService;
    this.ipWhitelistMapper = ipWhitelistMapper;
    this.tailscaleDeviceService = tailscaleDeviceService;
    this.tailscaleDeviceMapper = tailscaleDeviceMapper;
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
   * Returns the caller's role key strings.
   */
  @Transactional(readOnly = true)
  public List<String> myRoles() {
    UUID userId = UserContextHolder.getUserId();
    if (userId == null) return List.of();
    return accountRepository.findEffectiveRoleNames(userId);
  }

  /**
   * Returns an active remote account's effective permission patterns.
   *
   * @param remoteUserType the remote identity type
   * @param remoteUserId the remote identity id
   * @return the permission patterns, or empty when the account does not exist
   */
  @Transactional(readOnly = true)
  public List<String> effectivePermissions(RemoteUserType remoteUserType, String remoteUserId) {
    if (remoteUserType != RemoteUserType.DISCORD || remoteUserId == null || remoteUserId.isBlank()) {
      return List.of();
    }
    return accountRepository
        .findByProviderAndProviderIdAndStatus("discord", remoteUserId, AccountStatus.ACTIVE)
        .map(account -> accountRepository.findEffectivePermissions(account.getId()))
        .orElseGet(List::of);
  }

  /**
   * Property-set entries the remote user may execute.
   *
   * @param remoteUserType the remote identity type
   * @param remoteUserId the remote identity id
   * @param ownerKey the property-set owner
   * @param propertySet the property-set name
   * @return the accessible entries
   */
  @Transactional(readOnly = true)
  public List<PropertySetEntry> accessibleCommandIntents(
      RemoteUserType remoteUserType, String remoteUserId,
      String ownerKey, String propertySet) {
    if (remoteUserType != RemoteUserType.DISCORD || remoteUserId == null || remoteUserId.isBlank()) {
      return List.of();
    }
    return propertySetService
        .listAccessibleEntries(remoteUserId, ownerKey, propertySet)
        .stream()
        .map(propertySetMapper::map)
        .collect(Collectors.toList());
  }

  /**
   * Looks up every account across the system, paginated.
   */
  @Transactional(readOnly = true)
  public PagedAccounts accounts(PaginationInput pagination) {
    Pageable pageable = toPageable(pagination, "username", Sort.Direction.ASC);
    List<FilterSpec> filters = GraphQLSupport.toFilterSpecs(
        pagination == null ? null : pagination.getFilters(),
        f -> new FilterSpec(f.getField(), f.getOperator().name(), f.getValue()));
    Specification<AccountEntity> spec = FilterBuilder.buildFilters(filters);
    Page<AccountEntity> result = spec != null
        ? accountRepository.findAll(spec, pageable)
        : accountService.findAllPaged(pageable);
    List<Account> items = result.getContent().stream()
        .map(accountMapper::map)
        .toList();
    return PagedAccounts.newBuilder()
        .items(items)
        .pageInfo(toPageInfo(result))
        .build();
  }

  /**
   * Marks an account suspended.
   */
  @Transactional
  public QueryResult suspendAccount(String id) {
    AccountEntity account = accountService.findById(UUID.fromString(id))
        .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));
    account.setStatus(AccountStatus.SUSPENDED);
    accountService.save(account);
    logger.info("Suspended account {}", id);
    return QuerySuccess.newBuilder().message("Account suspended").id(id).build();
  }

  /**
   * Re-activates a suspended account.
   */
  @Transactional
  public QueryResult unsuspendAccount(String id) {
    AccountEntity account = accountService.findById(UUID.fromString(id))
        .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));
    account.setStatus(AccountStatus.ACTIVE);
    accountService.save(account);
    logger.info("Unsuspended account {}", id);
    return QuerySuccess.newBuilder().message("Account unsuspended").id(id).build();
  }

  /**
   * Deactivates the calling account, revoking its sessions.
   *
   * @return a QuerySuccess result
   */
  @Transactional
  public QueryResult deactivateAccount() {
    UUID userId = UserContextHolder.getUserId();
    if (userId == null) {
      return StandardError.newBuilder()
          .message("Not authenticated")
          .build();
    }
    accountService.deactivateAccount(userId);
    logger.info("Deactivated account {}", userId);
    return QuerySuccess.newBuilder()
        .message("Account deactivated")
        .id(userId.toString())
        .build();
  }

  /**
   * Emails a reactivation link to a deactivated account's address.
   *
   * <p>Always reports success to avoid email enumeration.
   *
   * @param email the account's email address
   * @return a QuerySuccess result
   */
  @Transactional
  public QueryResult requestAccountReactivation(String email, String provider) {
    return accountService.findByPersonEmail(email).stream()
        .filter(account -> account.getStatus() == AccountStatus.DEACTIVATED)
        .filter(account -> provider.equals(account.getProvider()))
        .findFirst()
        .map(account -> {
          try {
            var token = reactivationService.createToken(account.getId());
            String reactivationLink = resolveBaseUrl() + "/reactivate?token=" + token.getToken();
            emailService.sendReactivationEmail(email, reactivationLink);
            logger.info("Reactivation email sent for account {}", account.getId());
            return QuerySuccess.newBuilder()
                .message("Reactivation email sent")
                .id(account.getId().toString())
                .build();
          } catch (Exception e) {
            logger.error("Failed to send reactivation email for account {}", account.getId(), e);
            return StandardError.newBuilder()
                .message("Failed to send reactivation email")
                .build();
          }
        })
        .orElseGet(() -> {
          logger.warn("Reactivation requested for unknown or active email");
          return QuerySuccess.newBuilder()
              .message("Reactivation email sent")
              .build();
        });
  }

  /**
   * Reactivates an account using a confirmation token.
   *
   * @param token the reactivation token
   * @return the result of the reactivation
   */
  @Transactional
  public QueryResult confirmAccountReactivation(String token) {
    try {
      UUID accountId = reactivationService.useToken(token);
      AccountEntity account = accountService.findById(accountId)
          .orElseThrow(() -> new IllegalArgumentException("Account not found"));
      account.setStatus(AccountStatus.ACTIVE);
      accountService.save(account);
      logger.info("Reactivated account {}", accountId);
      return QuerySuccess.newBuilder()
          .message("Account reactivated")
          .id(accountId.toString())
          .build();
    } catch (Exception e) {
      return StandardError.newBuilder()
          .message(e.getMessage())
          .build();
    }
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
    return accountService.findByPersonEmail(email).stream()
        .filter(a -> a.getProvider() == null || "local".equals(a.getProvider()))
        .findFirst()
        .map(account -> {
          var token = passwordResetService.createToken(account.getId());
          String resetLink = resolveBaseUrl() + "/reset-password?token=" + token.getToken();
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
   * Issues a new API key for an account.
   *
   * @param accountUsername the account username
   * @param name            the key label
   * @return the issued key and its one-time plaintext
   */
  @Transactional
  public IssuedApiKey issueApiKey(String accountUsername, String name) {
    AccountEntity account = accountService.findByUsername(accountUsername)
        .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountUsername));
    ApiKeyService.ApiKeyIssue issue = apiKeyService.issueKey(account.getId(), name);
    logger.info("Issued API key {} for account {}", issue.apiKey().getId(), account.getId());
    return IssuedApiKey.newBuilder()
        .apiKey(apiKeyMapper.map(issue.apiKey()))
        .plaintextKey(issue.plaintextKey())
        .build();
  }

  /**
   * Disables an API key.
   *
   * @param id the key id
   * @return a success result
   */
  @Transactional
  public QueryResult revokeApiKey(String id) {
    apiKeyService.revoke(UUID.fromString(id));
    logger.info("Revoked API key {}", id);
    return QuerySuccess.newBuilder().message("API key revoked").id(id).build();
  }

  /**
   * Issues a fresh plaintext for an existing API key.
   *
   * @param id the key id
   * @return the rotated key and its one-time plaintext
   */
  @Transactional
  public IssuedApiKey rotateApiKey(String id) {
    ApiKeyService.ApiKeyIssue issue = apiKeyService.rotate(UUID.fromString(id));
    logger.info("Rotated API key {}", id);
    return IssuedApiKey.newBuilder()
        .apiKey(apiKeyMapper.map(issue.apiKey()))
        .plaintextKey(issue.plaintextKey())
        .build();
  }

  /**
   * Returns the stored hub registry, or a default when none is present.
   *
   * @return the hub registry
   */
  @Transactional(readOnly = true)
  public HubRegistry hubRegistry() {
    return propertySetService.getEntry(HUB_OWNER_KEY, HUB_SET_NAME, HUB_ENTRY_NAME)
        .map(PropertySetEntryEntity::getValues)
        .map(this::toHubRegistry)
        .orElseGet(this::defaultHubRegistry);
  }

  /**
   * Validates and persists the hub registry.
   *
   * @param input the hub registry input
   * @return the saved hub registry
   */
  @Transactional
  public HubRegistry saveRegistry(HubRegistryInput input) {
    if (input.getMode() == null) {
      throw new IllegalArgumentException("Hub mode is required");
    }
    List<Map<String, Object>> apps = new ArrayList<>();
    for (HubAppInput app : input.getApps()) {
      if (app.getKey() == null || app.getKey().isBlank()) {
        throw new IllegalArgumentException("Hub app key is required");
      }
      if (app.getDevPort() <= 0 || app.getProdPort() <= 0) {
        throw new IllegalArgumentException("Hub app ports must be positive");
      }
      apps.add(toAppValues(app));
    }
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("mode", input.getMode().name());
    values.put("apps", apps);
    propertySetService.upsertEntry(HUB_OWNER_KEY, HUB_SET_NAME, HUB_ENTRY_NAME, values, false);
    return toHubRegistry(values);
  }

  /**
   * Builds a default registry mirroring the node app's seed.
   *
   * @return the default registry
   */
  private HubRegistry defaultHubRegistry() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("mode", "dev");
    values.put("apps", List.of(
        appValues("sun", "Sun", ".", 5173, 5173,
            "https://sun.int.scarlettparker.co.uk", "Ecosystem home with blog and gallery", true, true),
        appValues("guided-reader", "Guided Reader", "../Guided-Reader", 5178, 5178,
            "http://localhost:5178", "Reading app with texts, annotations and a forum", true, false),
        appValues("checklist", "Checklist", "../Checklist", 5176, 5176,
            "https://checklist.int.scarlettparker.co.uk", "Structured checklists", true, false),
        appValues("viewer", "Viewer", "../Viewer", 5177, 5177,
            "https://viewer.int.scarlettparker.co.uk", "Interactive viewer", true, false),
        appValues("mame", "Emulator", "../MAME", 5175, 5180,
            "https://emulator.int.scarlettparker.co.uk", "Browser MAME emulator", true, false)));
    return toHubRegistry(values);
  }

  /**
   * Maps stored registry values into the typed registry.
   *
   * @param values the stored values
   * @return the typed registry
   */
  private HubRegistry toHubRegistry(Map<String, Object> values) {
    HubRegistry.Builder builder = HubRegistry.newBuilder();
    builder.mode(fromHubMode(String.valueOf(values.getOrDefault("mode", "dev"))));
    List<HubApp> apps = new ArrayList<>();
    if (values.get("apps") instanceof List<?> list) {
      for (Object item : list) {
        if (item instanceof Map<?, ?> appMap) {
          HubApp app = toHubApp(appMap);
          if (app != null) {
            apps.add(app);
          }
        }
      }
    }
    return builder.apps(apps).build();
  }

  /**
   * Maps a stored app map into the typed app, or null when the key is absent.
   *
   * @param map the stored app map
   * @return the typed app
   */
  private HubApp toHubApp(Map<?, ?> map) {
    Object key = map.get("key");
    if (key == null || String.valueOf(key).isBlank()) {
      return null;
    }
    String appKey = String.valueOf(key);
    HubApp.Builder builder = HubApp.newBuilder();
    builder.key(appKey);
    builder.name(strValue(map.get("name"), appKey));
    builder.dir(strValue(map.get("dir"), "../" + appKey));
    builder.devPort(intValue(map.get("devPort"), 5173));
    builder.prodPort(intValue(map.get("prodPort"), 5173));
    builder.url(strValue(map.get("url"), ""));
    builder.description(strValue(map.get("description"), ""));
    builder.enabled(!Boolean.FALSE.equals(map.get("enabled")));
    builder.self(Boolean.TRUE.equals(map.get("self")));
    return builder.build();
  }

  /**
   * Serialises a typed app input into a stored values map.
   *
   * @param app the app input
   * @return the stored app map
   */
  private Map<String, Object> toAppValues(HubAppInput app) {
    return appValues(app.getKey(), app.getName(), app.getDir(), app.getDevPort(),
        app.getProdPort(), app.getUrl(), app.getDescription(),
        app.getEnabled(), Boolean.TRUE.equals(app.getSelf()));
  }

  /**
   * Builds a stored app values map.
   *
   * @param key the app key
   * @param name the display name
   * @param dir the repo path
   * @param devPort the dev port
   * @param prodPort the prod port
   * @param url the public url
   * @param description the description
   * @param enabled whether the app is managed
   * @param self whether this is the current app
   * @return the stored app map
   */
  private Map<String, Object> appValues(String key, String name, String dir, int devPort,
      int prodPort, String url, String description, boolean enabled, boolean self) {
    Map<String, Object> app = new LinkedHashMap<>();
    app.put("key", key);
    app.put("name", name);
    app.put("dir", dir);
    app.put("devPort", devPort);
    app.put("prodPort", prodPort);
    app.put("url", url);
    app.put("description", description);
    app.put("enabled", enabled);
    app.put("self", self);
    return app;
  }

  /**
   * Resolves a stored mode string into the typed mode, defaulting to dev.
   *
   * @param value the stored mode
   * @return the typed mode
   */
  private HubMode fromHubMode(String value) {
    try {
      return HubMode.valueOf(value);
    } catch (IllegalArgumentException e) {
      return HubMode.dev;
    }
  }

  /**
   * Reads a string value with a fallback.
   *
   * @param value the stored value
   * @param fallback the fallback string
   * @return the string value
   */
  private String strValue(Object value, String fallback) {
    return value instanceof String s && !s.isBlank() ? s : fallback;
  }

  /**
   * Reads an integer value with a fallback.
   *
   * @param value the stored value
   * @param fallback the fallback integer
   * @return the integer value
   */
  private int intValue(Object value, int fallback) {
    return value instanceof Number n && n.intValue() > 0 ? n.intValue() : fallback;
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
   * Lists all IP whitelist entries.
   *
   * @return the list of entries
   */
  @Transactional(readOnly = true)
  public List<IpWhitelistEntry> ipWhitelistEntries() {
    return ipWhitelistMapper.map(ipWhitelistService.listAll());
  }

  /**
   * Creates a new IP whitelist entry.
   *
   * @param pattern     the IP pattern (CIDR, glob, or exact).
   * @param description optional description.
   * @return a success result with the entry id
   */
  @Transactional
  public QueryResult createIpWhitelistEntry(IpWhitelistEntryInput input) {
    IpWhitelistEntryEntity entity = ipWhitelistService.addEntry(
        input.getPattern(), input.getDescription(),
        input.getImmutable() != null && input.getImmutable());
    return QuerySuccess.newBuilder()
        .message("IP whitelist entry created")
        .id(entity.getId().toString())
        .build();
  }

  /**
   * Updates an existing IP whitelist entry.
   *
   * @param id    the entry id
   * @param input the updated fields
   * @return a success result
   */
  @Transactional
  public QueryResult updateIpWhitelistEntry(String id, IpWhitelistEntryInput input) {
    ipWhitelistService.updateEntry(UUID.fromString(id),
        input.getPattern(), input.getDescription(), input.getEnabled());
    return QuerySuccess.newBuilder()
        .message("IP whitelist entry updated")
        .id(id)
        .build();
  }

  /**
   * Deletes an IP whitelist entry.
   *
   * @param id the entry id
   * @return a success result
   */
  @Transactional
  public QueryResult deleteIpWhitelistEntry(String id) {
    ipWhitelistService.deleteEntry(UUID.fromString(id));
    return QuerySuccess.newBuilder()
        .message("IP whitelist entry deleted")
        .id(id)
        .build();
  }

  /**
   * Returns the calling app's base URL for emailed links, or the fallback.
   *
   * @return the app base URL
   */
  private String resolveBaseUrl() {
    try {
      ServletRequestAttributes attrs =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attrs != null) {
        String forwarded = attrs.getRequest().getHeader("X-App-Base-Url");
        if (forwarded != null && !forwarded.isBlank()) {
          return forwarded;
        }
      }
    } catch (Exception e) {
      // No servlet context available (e.g. test)
    }
    return appBaseUrl;
  }

  /**
   * Converts a GraphQL PaginationInput into a Spring Pageable.
   */
  private Pageable toPageable(PaginationInput pagination, String defaultSortBy,
      Sort.Direction defaultDir) {
    if (pagination == null) {
      return PageRequests.of(null, null, null, null, defaultSortBy, defaultDir);
    }
    return PageRequests.of(
        pagination.getPage(), pagination.getSize(),
        pagination.getSortBy(),
        pagination.getSortDir() == null ? null : pagination.getSortDir().name(),
        defaultSortBy, defaultDir);
  }

  /**
   * Converts a Spring Data page into GraphQL PageInfo.
   */
  private PageInfo toPageInfo(Page<?> page) {
    return PageInfo.newBuilder()
        .page(page.getNumber())
        .size(page.getSize())
        .totalPages(page.getTotalPages())
        .totalCount((int) page.getTotalElements())
        .hasNextPage(page.hasNext())
        .hasPreviousPage(page.hasPrevious())
        .build();
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

  /**
   * Returns all tracked Tailscale devices.
   */
  @Transactional(readOnly = true)
  public List<TailscaleDevice> tailscaleDevices() {
    return tailscaleDeviceMapper.map(tailscaleDeviceService.listAll());
  }

  /**
   * Returns a single Tailscale device by id.
   *
   * @param id the Gaia device record id.
   * @return the device, or null if not found.
   */
  @Transactional(readOnly = true)
  public TailscaleDevice tailscaleDevice(String id) {
    try {
      return tailscaleDeviceMapper.map(tailscaleDeviceService.findById(UUID.fromString(id)));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Marks a Tailscale device as expired.
   *
   * @param id the Gaia device record id.
   * @return a success result.
   */
  @Transactional
  public QueryResult expireTailscaleDevice(String id) {
    tailscaleDeviceService.markExpired(UUID.fromString(id));
    return QuerySuccess.newBuilder()
        .message("Tailscale device expired")
        .id(id)
        .build();
  }
}
