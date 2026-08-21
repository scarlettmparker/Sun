package com.sun.gaia.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsEnableDataFetcherInstrumentation;
import com.sun.gaia.codegen.types.Account;
import com.sun.gaia.codegen.types.AuthResult;
import com.sun.gaia.codegen.types.LoginInput;
import com.sun.gaia.codegen.types.PagedAccounts;
import com.sun.gaia.codegen.types.PaginationInput;
import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.codegen.types.RegisterInput;
import com.sun.gaia.graphql.services.AccountGraphQLService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Data fetchers for account operations.
 */
@DgsComponent
public class AccountDataFetcher {

  private final AccountGraphQLService accountGraphQLService;

  public AccountDataFetcher(AccountGraphQLService accountGraphQLService) {
    this.accountGraphQLService = accountGraphQLService;
  }

  /**
   * Returns the currently authenticated account.
   *
   * @return the Account object, or null if not authenticated
   */
  @DgsData(parentType = "GaiaQueries", field = "me")
  @PreAuthorize("@permissions.isAuthenticated()")
  public Account me() {
    return accountGraphQLService.me();
  }

  /**
   * Locates an account by id.
   *
   * @param id the account id
   * @return the Account object
   */
  @DgsData(parentType = "GaiaQueries", field = "account")
  @PreAuthorize("@permissions.has('graphql.gaia.account')")
  public Account account(String id) {
    return accountGraphQLService.account(id);
  }

  /**
   * Lists all accounts.
   *
   * @return a list of Account objects
   */
  @DgsData(parentType = "GaiaQueries", field = "listAccounts")
  @PreAuthorize("@permissions.has('graphql.gaia.listAccounts')")
  public List<Account> listAccounts() {
    return accountGraphQLService.listAccounts();
  }

  /**
   * Looks up every account across the system, paginated.
   */
  @DgsData(parentType = "GaiaQueries", field = "accounts")
  @PreAuthorize("@permissions.has('graphql.gaia.accounts')")
  public PagedAccounts accounts(PaginationInput pagination) {
    return accountGraphQLService.accounts(pagination);
  }

  /**
   * Suspends an account, revoking all active sessions.
   */
  @DgsData(parentType = "GaiaMutations", field = "suspendAccount")
  @PreAuthorize("@permissions.has('graphql.gaia.suspendAccount')")
  public QueryResult suspendAccount(String id) {
    return accountGraphQLService.suspendAccount(id);
  }

  /**
   * Re-activates a suspended account.
   */
  @DgsData(parentType = "GaiaMutations", field = "unsuspendAccount")
  @PreAuthorize("@permissions.has('graphql.gaia.unsuspendAccount')")
  public QueryResult unsuspendAccount(String id) {
    return accountGraphQLService.unsuspendAccount(id);
  }

  /**
   * Deactivates the calling account.
   */
  @DgsData(parentType = "GaiaMutations", field = "deactivateAccount")
  @PreAuthorize("@permissions.isAuthenticated()")
  public QueryResult deactivateAccount() {
    return accountGraphQLService.deactivateAccount();
  }

  /**
   * Registers a new account.
   *
   * @param input the registration input
   * @return the auth result with JWT token
   */
  @DgsData(parentType = "GaiaMutations", field = "register")
  @PreAuthorize("permitAll()")
  @DgsEnableDataFetcherInstrumentation(false)
  public AuthResult register(RegisterInput input) {
    return accountGraphQLService.register(input);
  }

  /**
   * Authenticates an account.
   *
   * @param input the login input
   * @return the auth result with JWT token
   */
  @DgsData(parentType = "GaiaMutations", field = "login")
  @PreAuthorize("permitAll()")
  @DgsEnableDataFetcherInstrumentation(false)
  public AuthResult login(LoginInput input) {
    return accountGraphQLService.login(input);
  }

  /**
   * Logs out the current account.
   *
   * @return the result of the logout operation
   */
  @DgsData(parentType = "GaiaMutations", field = "logout")
  @PreAuthorize("@permissions.isAuthenticated()")
  public QueryResult logout() {
    return accountGraphQLService.logout();
  }

  /**
   * Requests a password reset email.
   *
   * @param email the email to send the reset link to
   * @return the result of the request
   */
  @DgsData(parentType = "GaiaMutations", field = "requestPasswordReset")
  @PreAuthorize("permitAll()")
  @DgsEnableDataFetcherInstrumentation(false)
  public QueryResult requestPasswordReset(String email) {
    return accountGraphQLService.requestPasswordReset(email);
  }

  /**
   * Resets a password using a reset token.
   *
   * @param token the reset token
   * @param newPassword the new password
   * @return the result of the reset operation
   */
  @DgsData(parentType = "GaiaMutations", field = "resetPassword")
  @PreAuthorize("permitAll()")
  @DgsEnableDataFetcherInstrumentation(false)
  public QueryResult resetPassword(String token, String newPassword) {
    return accountGraphQLService.resetPassword(token, newPassword);
  }

  /**
   * Changes the password for the current account.
   *
   * @param currentPassword the current password
   * @param newPassword the new password
   * @return the result of the change operation
   */
  @DgsData(parentType = "GaiaMutations", field = "changePassword")
  @PreAuthorize("@permissions.isAuthenticated()")
  public QueryResult changePassword(String currentPassword, String newPassword) {
    return accountGraphQLService.changePassword(currentPassword, newPassword);
  }

  /**
   * Emails a reactivation link to a deactivated account of the given provider.
   */
  @DgsData(parentType = "GaiaMutations", field = "requestAccountReactivation")
  @PreAuthorize("permitAll()")
  @DgsEnableDataFetcherInstrumentation(false)
  public QueryResult requestAccountReactivation(String email, String provider) {
    return accountGraphQLService.requestAccountReactivation(email, provider);
  }

  /**
   * Reactivates an account using a confirmation token.
   */
  @DgsData(parentType = "GaiaMutations", field = "confirmAccountReactivation")
  @PreAuthorize("permitAll()")
  @DgsEnableDataFetcherInstrumentation(false)
  public QueryResult confirmAccountReactivation(String token) {
    return accountGraphQLService.confirmAccountReactivation(token);
  }
}
