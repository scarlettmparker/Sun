package com.sun.gaia.graphql.services;

import com.sun.fates.model.PersonEntity;
import com.sun.fates.service.PersonService;
import com.sun.gaia.codegen.types.Account;
import com.sun.gaia.codegen.types.AuthResult;
import com.sun.gaia.codegen.types.LoginInput;
import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.codegen.types.QuerySuccess;
import com.sun.gaia.codegen.types.RegisterInput;
import com.sun.gaia.codegen.types.StandardError;
import com.sun.gaia.graphql.mappers.AccountMapper;
import com.sun.gaia.model.AccountEntity;
import com.sun.gaia.service.AccountService;
import com.sun.gaia.service.EmailService;
import com.sun.gaia.service.JwtService;
import com.sun.gaia.service.PasswordResetService;
import com.sun.gaia.service.UserContextHolder;
import java.util.List;
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
  private final String appBaseUrl;

  public GaiaGraphQLService(AccountService accountService, PersonService personService,
      JwtService jwtService, EmailService emailService,
      PasswordResetService passwordResetService, AccountMapper accountMapper,
      @Value("${app.base-url:http://localhost:5176}") String appBaseUrl) {
    this.accountService = accountService;
    this.personService = personService;
    this.jwtService = jwtService;
    this.emailService = emailService;
    this.passwordResetService = passwordResetService;
    this.accountMapper = accountMapper;
    this.appBaseUrl = appBaseUrl;
  }

  /**
   * Returns the currently authenticated account.
   *
   * @return the GraphQL Account, or null if not authenticated
   */
  @Transactional(value = "gaiaTransactionManager", readOnly = true)
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
  @Transactional(value = "gaiaTransactionManager", readOnly = true)
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
  @Transactional(value = "gaiaTransactionManager", readOnly = true)
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
  @Transactional("gaiaTransactionManager")
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
  @Transactional("gaiaTransactionManager")
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
  @Transactional("gaiaTransactionManager")
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
  @Transactional("gaiaTransactionManager")
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
}
