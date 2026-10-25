package com.sun.gaia.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsEnableDataFetcherInstrumentation;
import com.sun.base.audit.annotation.Audited;
import com.sun.base.error.MutationException;
import com.sun.base.util.FilterBuilder;
import com.sun.base.util.FilterSpec;
import com.sun.base.util.GraphQLSupport;
import com.sun.fates.model.PersonEntity;
import com.sun.fates.service.PersonService;
import com.sun.gaia.codegen.types.Account;
import com.sun.gaia.codegen.types.AuthResult;
import com.sun.gaia.codegen.types.ChangePasswordResponse;
import com.sun.gaia.codegen.types.ConfirmAccountReactivationResponse;
import com.sun.gaia.codegen.types.DeactivateAccountResponse;
import com.sun.gaia.codegen.types.LoginInput;
import com.sun.gaia.codegen.types.LogoutResponse;
import com.sun.gaia.codegen.types.PagedAccounts;
import com.sun.gaia.codegen.types.PaginationInput;
import com.sun.gaia.codegen.types.RegisterInput;
import com.sun.gaia.codegen.types.RequestAccountReactivationResponse;
import com.sun.gaia.codegen.types.RequestMagicLinkResponse;
import com.sun.gaia.codegen.types.RequestPasswordResetResponse;
import com.sun.gaia.codegen.types.ResetPasswordResponse;
import com.sun.gaia.codegen.types.SuspendAccountResponse;
import com.sun.gaia.codegen.types.UnsuspendAccountResponse;
import com.sun.gaia.graphql.mappers.AccountMapper;
import com.sun.gaia.graphql.services.support.GaiaGraphQLSupport;
import com.sun.gaia.model.AccountEntity;
import com.sun.gaia.model.enums.AccountStatus;
import com.sun.gaia.repository.AccountRepository;
import com.sun.gaia.service.AccountService;
import com.sun.gaia.service.EmailService;
import com.sun.gaia.service.GoogleOAuthService;
import com.sun.gaia.service.JwtService;
import com.sun.gaia.service.MagicLinkService;
import com.sun.gaia.service.PasswordResetService;
import com.sun.gaia.service.ReactivationService;
import com.sun.gaia.service.UserContextHolder;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Data fetchers for account operations.
 */
@DgsComponent
public class AccountDataFetcher {

  private static final Logger logger = LoggerFactory.getLogger(AccountDataFetcher.class);

  private final AccountService accountService;
  private final AccountRepository accountRepository;
  private final PersonService personService;
  private final JwtService jwtService;
  private final EmailService emailService;
  private final PasswordResetService passwordResetService;
  private final ReactivationService reactivationService;
  private final MagicLinkService magicLinkService;
  private final GoogleOAuthService googleOAuthService;
  private final AccountMapper accountMapper;
  private final String appBaseUrl;

  public AccountDataFetcher(
      AccountService accountService,
      AccountRepository accountRepository,
      PersonService personService,
      JwtService jwtService,
      EmailService emailService,
      PasswordResetService passwordResetService,
      ReactivationService reactivationService,
      MagicLinkService magicLinkService,
      GoogleOAuthService googleOAuthService,
      AccountMapper accountMapper,
      @Value("${app.base-url}") String appBaseUrl) {
    this.accountService = accountService;
    this.accountRepository = accountRepository;
    this.personService = personService;
    this.jwtService = jwtService;
    this.emailService = emailService;
    this.passwordResetService = passwordResetService;
    this.reactivationService = reactivationService;
    this.magicLinkService = magicLinkService;
    this.googleOAuthService = googleOAuthService;
    this.accountMapper = accountMapper;
    this.appBaseUrl = appBaseUrl;
  }

  /**
   * Returns the currently authenticated account.
   *
   * @return the Account object, or null if not authenticated
   */
  @Audited(event = "ACCOUNT_VIEW", entity = "Account")
  @DgsData(parentType = "GaiaQueries", field = "me")
  @PreAuthorize("@permissions.isAuthenticated()")
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
   * @return the Account object
   */
  @Audited(event = "ACCOUNT_VIEW", entity = "Account")
  @DgsData(parentType = "GaiaQueries", field = "account")
  @PreAuthorize("@permissions.has('graphql.gaia.account')")
  @Transactional(readOnly = true)
  public Account account(String id) {
    return accountService.findById(UUID.fromString(id))
        .map(accountMapper::map)
        .orElse(null);
  }

  /**
   * Lists all accounts.
   *
   * @return a list of Account objects
   */
  @Audited(event = "ACCOUNT_LIST", entity = "Account")
  @DgsData(parentType = "GaiaQueries", field = "listAccounts")
  @PreAuthorize("@permissions.has('graphql.gaia.listAccounts')")
  @Transactional(readOnly = true)
  public List<Account> listAccounts() {
    return accountService.findAll().stream()
        .map(accountMapper::map)
        .collect(Collectors.toList());
  }

  /**
   * Looks up every account across the system, paginated.
   */
  @Audited(event = "ACCOUNT_LIST", entity = "Account")
  @DgsData(parentType = "GaiaQueries", field = "accounts")
  @PreAuthorize("@permissions.has('graphql.gaia.accounts')")
  @Transactional(readOnly = true)
  public PagedAccounts accounts(PaginationInput pagination) {
    Pageable pageable = GaiaGraphQLSupport.toPageable(pagination, "username", Sort.Direction.ASC);
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
        .pageInfo(GaiaGraphQLSupport.toPageInfo(result))
        .build();
  }

  /**
   * Suspends an account, revoking all active sessions.
   */
  @Audited(event = "ACCOUNT_SUSPEND", entity = "Account")
  @DgsData(parentType = "GaiaMutations", field = "suspendAccount")
  @PreAuthorize("@permissions.has('graphql.gaia.suspendAccount')")
  @Transactional
  public SuspendAccountResponse suspendAccount(String id) {
    AccountEntity account = accountService.findById(UUID.fromString(id))
        .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));
    account.setStatus(AccountStatus.SUSPENDED);
    accountService.save(account);
    try {
      jwtService.revokeAllForAccount(UUID.fromString(id));
    } catch (Exception e) {
      logger.warn("Failed to revoke sessions for suspended account {}", id, e);
    }
    logger.info("Suspended account {}", id);
    return SuspendAccountResponse.newBuilder()
        .message("Account suspended")
        .account(accountMapper.map(account))
        .build();
  }

  /**
   * Re-activates a suspended account.
   */
  @Audited(event = "ACCOUNT_UNSUSPEND", entity = "Account")
  @DgsData(parentType = "GaiaMutations", field = "unsuspendAccount")
  @PreAuthorize("@permissions.has('graphql.gaia.unsuspendAccount')")
  @Transactional
  public UnsuspendAccountResponse unsuspendAccount(String id) {
    AccountEntity account = accountService.findById(UUID.fromString(id))
        .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));
    account.setStatus(AccountStatus.ACTIVE);
    accountService.save(account);
    logger.info("Unsuspended account {}", id);
    return UnsuspendAccountResponse.newBuilder()
        .message("Account unsuspended")
        .account(accountMapper.map(account))
        .build();
  }

  /**
   * Deactivates the calling account.
   */
  @Audited(event = "ACCOUNT_DEACTIVATE", entity = "Account")
  @DgsData(parentType = "GaiaMutations", field = "deactivateAccount")
  @PreAuthorize("@permissions.isAuthenticated()")
  @Transactional
  public DeactivateAccountResponse deactivateAccount() {
    UUID userId = UserContextHolder.getUserId();
    if (userId == null) {
      throw new MutationException("Not authenticated");
    }
    accountService.deactivateAccount(userId);
    try {
      jwtService.revokeAllForAccount(userId);
      ServletRequestAttributes attrs =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attrs != null) {
        String authHeader = attrs.getRequest().getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
          jwtService.revokeToken(authHeader.substring(7));
        }
      }
    } catch (Exception e) {
      logger.warn("Failed to revoke token on deactivate {}", userId, e);
    }
    logger.info("Deactivated account {}", userId);
    return DeactivateAccountResponse.newBuilder()
        .message("Account deactivated")
        .build();
  }

  /**
   * Registers a new account.
   *
   * @param input the registration input
   * @return the auth result with JWT token
   */
  @Audited(event = "ACCOUNT_REGISTER", entity = "Account",
      sensitiveFields = {"email", "firstName", "lastName"})
  @DgsData(parentType = "GaiaMutations", field = "register")
  @PreAuthorize("permitAll()")
  @DgsEnableDataFetcherInstrumentation(false)
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
  @Audited(event = "ACCOUNT_LOGIN", entity = "Account")
  @DgsData(parentType = "GaiaMutations", field = "login")
  @PreAuthorize("permitAll()")
  @DgsEnableDataFetcherInstrumentation(false)
  public AuthResult login(LoginInput input) {
    AccountEntity account = accountService.findByLoginIdentifier(input.getUsername())
        .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

    if (account.getStatus() != AccountStatus.ACTIVE) {
      throw new IllegalArgumentException("Invalid username or password");
    }

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
   * @return the result of the logout operation
   */
  @Audited(event = "ACCOUNT_LOGOUT", entity = "Account")
  @DgsData(parentType = "GaiaMutations", field = "logout")
  @PreAuthorize("@permissions.isAuthenticated()")
  public LogoutResponse logout() {
    try {
      ServletRequestAttributes attrs =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attrs != null) {
        String authHeader = attrs.getRequest().getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
          String token = authHeader.substring(7);
          jwtService.revokeToken(token);
        }
      }
    } catch (Exception e) {
      logger.warn("Failed to revoke token on logout", e);
    }
    return LogoutResponse.newBuilder()
        .message("Logout succeeded")
        .build();
  }

  /**
   * Requests a password reset email.
   *
   * @param email the email to send the reset link to
   * @return the result of the request
   */
  @Audited(event = "ACCOUNT_REQUESTPASSWORDRESET", entity = "Account")
  @DgsData(parentType = "GaiaMutations", field = "requestPasswordReset")
  @PreAuthorize("permitAll()")
  @DgsEnableDataFetcherInstrumentation(false)
  @Transactional
  public RequestPasswordResetResponse requestPasswordReset(String email) {
    return accountService.findByPersonEmail(email).stream()
        .filter(a -> a.getProvider() == null || "local".equals(a.getProvider()))
        .findFirst()
        .map(account -> {
          var token = passwordResetService.createToken(account.getId());
          String resetLink = GaiaGraphQLSupport.resolveBaseUrl(appBaseUrl) + "/reset-password?token=" + token.getToken();
          emailService.sendPasswordResetEmail(email, resetLink);
          return RequestPasswordResetResponse.newBuilder()
              .message("Password reset email sent")
              .build();
        })
        .orElseGet(() -> {
          logger.warn("Password reset requested for unknown email");
          return RequestPasswordResetResponse.newBuilder()
              .message("Password reset email sent")
              .build();
        });
  }

  /**
   * Requests a magic-link sign-in email. Always reports success to avoid account enumeration.
   */
  @Audited(event = "ACCOUNT_REQUESTMAGICLINK", entity = "Account")
  @DgsData(parentType = "GaiaMutations", field = "requestMagicLink")
  @PreAuthorize("permitAll()")
  @DgsEnableDataFetcherInstrumentation(false)
  @Transactional
  public RequestMagicLinkResponse requestMagicLink(String email) {
    accountService.findByPersonEmail(email).stream()
        .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
        .findFirst()
        .ifPresent(account -> {
          var token = magicLinkService.createToken(account.getId());
          String link = GaiaGraphQLSupport.resolveBaseUrl(appBaseUrl)
              + "/__magic-link/verify?token=" + token.getToken();
          emailService.sendMagicLinkEmail(email, link);
        });
    return RequestMagicLinkResponse.newBuilder()
        .message("Check your inbox")
        .build();
  }

  /**
   * Consumes a magic-link token and returns a JWT.
   */
  @Audited(event = "ACCOUNT_VERIFYMAGICLINK", entity = "Account")
  @DgsData(parentType = "GaiaMutations", field = "verifyMagicLink")
  @PreAuthorize("permitAll()")
  @DgsEnableDataFetcherInstrumentation(false)
  @Transactional
  public AuthResult verifyMagicLink(String token) {
    UUID accountId = magicLinkService.useToken(token);
    AccountEntity account = accountService.findById(accountId)
        .orElseThrow(() -> new IllegalArgumentException("Invalid magic link token"));
    if (account.getStatus() != AccountStatus.ACTIVE) {
      throw new IllegalArgumentException("Account not active");
    }
    String jwt = jwtService.generateToken(account.getId(), account.getPersonId());
    logger.info("Magic link login succeeded for account {}", account.getId());
    return AuthResult.newBuilder()
        .accountId(account.getId().toString())
        .personId(account.getPersonId().toString())
        .token(jwt)
        .build();
  }

  /**
   * Signs in with Google using an authorization code.
   */
  @Audited(event = "ACCOUNT_GOOGLELOGIN", entity = "Account")
  @DgsData(parentType = "GaiaMutations", field = "loginWithGoogle")
  @PreAuthorize("permitAll()")
  @DgsEnableDataFetcherInstrumentation(false)
  @Transactional
  public AuthResult loginWithGoogle(String code, String redirectUri) {
    var googleUser = googleOAuthService.exchangeCode(code, redirectUri)
        .orElseThrow(() -> new IllegalArgumentException("Google sign-in failed"));

    AccountEntity account = accountService.findByPersonEmail(googleUser.email()).stream()
        .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No account for this email"));

    // Link provider id if missing.
    if (account.getProvider() == null || account.getProvider().isBlank()) {
      account.setProvider("google");
      account.setProviderId(googleUser.sub());
      accountService.save(account);
    }

    String jwt = jwtService.generateToken(account.getId(), account.getPersonId());
    logger.info("Google login succeeded for account {}", account.getId());
    return AuthResult.newBuilder()
        .accountId(account.getId().toString())
        .personId(account.getPersonId().toString())
        .token(jwt)
        .build();
  }

  /**
   * Resets a password using a reset token.
   *
   * @param token the reset token
   * @param newPassword the new password
   * @return the result of the reset operation
   */
  @Audited(event = "ACCOUNT_RESETPASSWORD", entity = "Account")
  @DgsData(parentType = "GaiaMutations", field = "resetPassword")
  @PreAuthorize("permitAll()")
  @DgsEnableDataFetcherInstrumentation(false)
  @Transactional
  public ResetPasswordResponse resetPassword(String token, String newPassword) {
    UUID accountId = passwordResetService.useToken(token);
    accountService.changePassword(accountId, newPassword);
    return ResetPasswordResponse.newBuilder()
        .message("Password reset succeeded")
        .build();
  }

  /**
   * Changes the password for the current account.
   *
   * @param currentPassword the current password
   * @param newPassword the new password
   * @return the result of the change operation
   */
  @Audited(event = "ACCOUNT_CHANGEPASSWORD", entity = "Account")
  @DgsData(parentType = "GaiaMutations", field = "changePassword")
  @PreAuthorize("@permissions.isAuthenticated()")
  @Transactional
  public ChangePasswordResponse changePassword(String currentPassword, String newPassword) {
    UUID userId = UserContextHolder.getUserId();
    if (userId == null) {
      throw new MutationException("Not authenticated");
    }

    AccountEntity account = accountService.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("Account not found"));

    if (!accountService.verifyPassword(account, currentPassword)) {
      throw new MutationException("Current password incorrect");
    }

    accountService.changePassword(userId, newPassword);
    return ChangePasswordResponse.newBuilder()
        .message("Password changed")
        .build();
  }

  /**
   * Emails a reactivation link to a deactivated account of the given provider.
   */
  @Audited(event = "ACCOUNT_REQUESTREACTIVATION", entity = "Account")
  @DgsData(parentType = "GaiaMutations", field = "requestAccountReactivation")
  @PreAuthorize("permitAll()")
  @DgsEnableDataFetcherInstrumentation(false)
  @Transactional
  public RequestAccountReactivationResponse requestAccountReactivation(String email, String provider) {
    return accountService.findByPersonEmail(email).stream()
        .filter(account -> account.getStatus() == AccountStatus.DEACTIVATED)
        .filter(account -> provider.equals(account.getProvider()))
        .findFirst()
        .map(account -> {
          var token = reactivationService.createToken(account.getId());
          String reactivationLink = GaiaGraphQLSupport.resolveBaseUrl(appBaseUrl) + "/reactivate?token=" + token.getToken();
          emailService.sendReactivationEmail(email, reactivationLink);
          logger.info("Reactivation email sent for account {}", account.getId());
          return RequestAccountReactivationResponse.newBuilder()
              .message("Reactivation email sent")
              .build();
        })
        .orElseGet(() -> {
          logger.warn("Reactivation requested for unknown or active email");
          return RequestAccountReactivationResponse.newBuilder()
              .message("Reactivation email sent")
              .build();
        });
  }

  /**
   * Reactivates an account using a confirmation token.
   */
  @Audited(event = "ACCOUNT_CONFIRMREACTIVATION", entity = "Account")
  @DgsData(parentType = "GaiaMutations", field = "confirmAccountReactivation")
  @PreAuthorize("permitAll()")
  @DgsEnableDataFetcherInstrumentation(false)
  @Transactional
  public ConfirmAccountReactivationResponse confirmAccountReactivation(String token) {
    UUID accountId = reactivationService.useToken(token);
    AccountEntity account = accountService.findById(accountId)
        .orElseThrow(() -> new IllegalArgumentException("Account not found"));
    account.setStatus(AccountStatus.ACTIVE);
    accountService.save(account);
    logger.info("Reactivated account {}", accountId);
    return ConfirmAccountReactivationResponse.newBuilder()
        .message("Account reactivated")
        .build();
  }
}
