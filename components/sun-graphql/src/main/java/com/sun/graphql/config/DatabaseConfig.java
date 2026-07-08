package com.sun.graphql.config;

import com.sun.gaia.graphql.config.AuditorAwareImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.UUID;

/**
 * Single shared JPA configuration for the whole gateway.
 *
 * <p>Every stateful component shares one {@link javax.sql.DataSource} (from
 * {@code spring.datasource.*}), one auto-configured {@code EntityManagerFactory} that scans
 * every {@code @Entity} under {@code com.sun} (via {@code @EntityScan} on the application
 * class), and one auto-configured {@code TransactionManager}. Components are enabled simply by
 * being on the classpath &mdash; see the {@code dbModules} list in
 * {@code sun-graphql/build.gradle}. Remove a module there to disable it.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableJpaRepositories(basePackages = "com.sun")
public class DatabaseConfig {

  @Bean
  public AuditorAware<UUID> auditorProvider() {
    return new AuditorAwareImpl();
  }
}
