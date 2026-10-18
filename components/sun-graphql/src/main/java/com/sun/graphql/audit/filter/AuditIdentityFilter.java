package com.sun.graphql.audit.filter;

import com.sun.base.audit.context.AuditContext;
import com.sun.gaia.model.AccountRoleEntity;
import com.sun.gaia.model.RoleEntity;
import com.sun.gaia.repository.AccountRepository;
import com.sun.gaia.repository.AccountRoleRepository;
import com.sun.gaia.repository.RoleRepository;
import com.sun.gaia.service.UserContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Copies the caller's identity into {@link AuditContext} during the request. The
 * outer capture filter cannot read {@link UserContextHolder} in its finally block
 * because the auth filter has already cleared it by then.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 51)
public class AuditIdentityFilter extends OncePerRequestFilter {

  private final AuditContext auditContext;
  private final AccountRepository accountRepository;
  private final AccountRoleRepository accountRoleRepository;
  private final RoleRepository roleRepository;

  public AuditIdentityFilter(AuditContext auditContext,
                             AccountRepository accountRepository,
                             AccountRoleRepository accountRoleRepository,
                             RoleRepository roleRepository) {
    this.auditContext = auditContext;
    this.accountRepository = accountRepository;
    this.accountRoleRepository = accountRoleRepository;
    this.roleRepository = roleRepository;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (auditContext.isActive()) {
      UUID userId = UserContextHolder.getUserId();
      auditContext.setUserId(userId);
      if (userId != null) {
        accountRepository.findById(userId).ifPresent(account -> {
          auditContext.setUserName(account.getUsername());
          auditContext.setUserRoles(roleNames(userId));
        });
      }
    }
    chain.doFilter(request, response);
  }

  /**
   * Resolves the caller's role names.
   *
   * @param accountId the account id
   * @return the role names, empty when the account holds none
   */
  private List<String> roleNames(UUID accountId) {
    List<UUID> roleIds = accountRoleRepository.findByAccountId(accountId).stream()
        .map(AccountRoleEntity::getRoleId)
        .toList();
    if (roleIds.isEmpty()) {
      return List.of();
    }
    return roleRepository.findAllById(roleIds).stream()
        .map(RoleEntity::getName)
        .toList();
  }
}
