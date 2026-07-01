package com.sun.gaia.graphql.config;

import com.sun.gaia.service.UserContextHolder;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.AuditorAware;

public class AuditorAwareImpl implements AuditorAware<UUID> {

  @Override
  public Optional<UUID> getCurrentAuditor() {
    return Optional.ofNullable(UserContextHolder.getUserId());
  }
}
