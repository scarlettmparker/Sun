package com.sun.graphql.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DatabaseConfig {

  @Configuration
  @EnableJpaRepositories(basePackages = "com.sun.apollo.repository", entityManagerFactoryRef = "apolloEntityManagerFactory", transactionManagerRef = "apolloTransactionManager")
  public static class ApolloJpaConfig {
  }

  @Configuration
  @EnableJpaRepositories(basePackages = "com.sun.briareus.repository", entityManagerFactoryRef = "briareusEntityManagerFactory", transactionManagerRef = "briareusTransactionManager")
  public static class BriareusJpaConfig {
  }

  @Bean(name = "apolloDataSource")
  @ConfigurationProperties(prefix = "spring.datasource.apollo")
  public DataSource apolloDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean(name = "briareusDataSource")
  @ConfigurationProperties(prefix = "spring.datasource.briareus")
  public DataSource briareusDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean(name = "apolloEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean apolloEntityManagerFactory(
      @Qualifier("apolloDataSource") DataSource dataSource) {
    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
    em.setDataSource(dataSource);
    em.setPackagesToScan("com.sun.apollo.model");

    HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    em.setJpaVendorAdapter(vendorAdapter);

    Map<String, Object> properties = new HashMap<>();
    properties.put("hibernate.hbm2ddl.auto", "update");
    properties.put("hibernate.show_sql", "true");
    properties.put("hibernate.format_sql", "true");
    properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
    em.setJpaPropertyMap(properties);

    return em;
  }

  @Bean(name = "briareusEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean briareusEntityManagerFactory(
      @Qualifier("briareusDataSource") DataSource dataSource) {
    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
    em.setDataSource(dataSource);
    em.setPackagesToScan("com.sun.briareus.model");

    HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    em.setJpaVendorAdapter(vendorAdapter);

    Map<String, Object> properties = new HashMap<>();
    properties.put("hibernate.hbm2ddl.auto", "update");
    properties.put("hibernate.show_sql", "true");
    properties.put("hibernate.format_sql", "true");
    properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
    em.setJpaPropertyMap(properties);

    return em;
  }

  @Bean(name = "apolloTransactionManager")
  public PlatformTransactionManager apolloTransactionManager(
      @Qualifier("apolloEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
    return new JpaTransactionManager(entityManagerFactory.getObject());
  }

  @Bean(name = "briareusTransactionManager")
  public PlatformTransactionManager briareusTransactionManager(
      @Qualifier("briareusEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
    return new JpaTransactionManager(entityManagerFactory.getObject());
  }
}