package com.sun.gaia.service;

import com.sun.gaia.model.AccountEntity;
import com.sun.gaia.repository.AccountRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class GaiaUserDetailsService implements UserDetailsService {

  private final AccountService accountService;
  private final AccountRepository accountRepository;

  /**
   * Creates the service.
   *
   * @param accountService account service
   * @param accountRepository account repository
   */
  public GaiaUserDetailsService(AccountService accountService, AccountRepository accountRepository) {
    this.accountService = accountService;
    this.accountRepository = accountRepository;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    AccountEntity account = accountService.findByLoginIdentifier(username)
        .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    if (account.getStatus() != com.sun.gaia.model.enums.AccountStatus.ACTIVE) {
      throw new UsernameNotFoundException("Account not active");
    }

    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
    for (String role : accountRepository.findEffectiveRoleNames(account.getId())) {
      authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
    }
    for (String perm : accountRepository.findEffectivePermissions(account.getId())) {
      authorities.add(new SimpleGrantedAuthority(perm));
    }

    return User.withUsername(account.getUsername())
        .password(account.getPasswordHash())
        .authorities(authorities)
        .build();
  }
}