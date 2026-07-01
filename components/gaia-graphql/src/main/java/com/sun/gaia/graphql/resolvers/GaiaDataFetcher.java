package com.sun.gaia.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsEnableDataFetcherInstrumentation;
import com.sun.gaia.codegen.types.Account;
import com.sun.gaia.codegen.types.AuthResult;
import com.sun.gaia.codegen.types.GaiaMutations;
import com.sun.gaia.codegen.types.GaiaQueries;
import com.sun.gaia.codegen.types.LoginInput;
import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.codegen.types.RegisterInput;
import com.sun.gaia.graphql.services.GaiaGraphQLService;
import com.sun.gaia.service.JwtService;
import com.sun.gaia.service.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Data fetchers for the access queries and mutations.
 */
@DgsComponent
public class GaiaDataFetcher {

  @Autowired
  private GaiaGraphQLService gaiaGraphQLService;

  @Autowired
  private JwtService jwtService;

  /**
   * Provides the access queries object.
   *
   * @return the GaiaQueries builder
   */
  @DgsData(parentType = "Query", field = "gaiaQueries")
  public GaiaQueries getGaiaQueries() {
    resolveUserFromRequest();
    return GaiaQueries.newBuilder().build();
  }

  /**
   * Returns the currently authenticated account.
   *
   * @return the Account object, or null if not authenticated
   */
  @DgsData(parentType = "GaiaQueries", field = "me")
  public Account me() {
    return gaiaGraphQLService.me();
  }

  /**
   * Locates an account by id.
   *
   * @param id the account id
   * @return the Account object
   */
  @DgsData(parentType = "GaiaQueries", field = "account")
  public Account account(String id) {
    return gaiaGraphQLService.account(id);
  }

  /**
   * Lists all accounts.
   *
   * @return a list of Account objects
   */
  @DgsData(parentType = "GaiaQueries", field = "listAccounts")
  public List<Account> listAccounts() {
    return gaiaGraphQLService.listAccounts();
  }

  /**
   * Provides the access mutations object.
   *
   * @return the GaiaMutations builder
   */
  @DgsData(parentType = "Mutation", field = "gaiaMutations")
  public GaiaMutations getGaiaMutations() {
    resolveUserFromRequest();
    return GaiaMutations.newBuilder().build();
  }

  /**
   * Registers a new account.
   *
   * @param input the registration input
   * @return the auth result with JWT token
   */
  @DgsData(parentType = "GaiaMutations", field = "register")
  @DgsEnableDataFetcherInstrumentation(false)
  public AuthResult register(RegisterInput input) {
    return gaiaGraphQLService.register(input);
  }

  /**
   * Authenticates an account.
   *
   * @param input the login input
   * @return the auth result with JWT token
   */
  @DgsData(parentType = "GaiaMutations", field = "login")
  @DgsEnableDataFetcherInstrumentation(false)
  public AuthResult login(LoginInput input) {
    return gaiaGraphQLService.login(input);
  }

  /**
   * Logs out the current account.
   *
   * @return the result of the logout operation
   */
  @DgsData(parentType = "GaiaMutations", field = "logout")
  public QueryResult logout() {
    return gaiaGraphQLService.logout();
  }

  /**
   * Requests a password reset email.
   *
   * @param email the email to send the reset link to
   * @return the result of the request
   */
  @DgsData(parentType = "GaiaMutations", field = "requestPasswordReset")
  @DgsEnableDataFetcherInstrumentation(false)
  public QueryResult requestPasswordReset(String email) {
    return gaiaGraphQLService.requestPasswordReset(email);
  }

  /**
   * Resets a password using a reset token.
   *
   * @param token the reset token
   * @param newPassword the new password
   * @return the result of the reset operation
   */
  @DgsData(parentType = "GaiaMutations", field = "resetPassword")
  @DgsEnableDataFetcherInstrumentation(false)
  public QueryResult resetPassword(String token, String newPassword) {
    return gaiaGraphQLService.resetPassword(token, newPassword);
  }

  /**
   * Changes the password for the current account.
   *
   * @param currentPassword the current password
   * @param newPassword the new password
   * @return the result of the change operation
   */
  @DgsData(parentType = "GaiaMutations", field = "changePassword")
  public QueryResult changePassword(String currentPassword, String newPassword) {
    return gaiaGraphQLService.changePassword(currentPassword, newPassword);
  }

  private void resolveUserFromRequest() {
    try {
      ServletRequestAttributes attrs =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attrs == null) {
        return;
      }
      HttpServletRequest request = attrs.getRequest();
      String authHeader = request.getHeader("Authorization");
      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7);
        if (jwtService.isValid(token)) {
          UUID accountId = jwtService.extractAccountId(token);
          UserContextHolder.setUserId(accountId);
        }
      }
    } catch (Exception e) {
      // No servlet context available (e.g. test)
    }
  }
}
