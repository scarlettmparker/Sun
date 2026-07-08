package com.sun.graphql.config;

import com.sun.gaia.graphql.config.AuditorAwareImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.config.AuditingConfiguration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class DatabaseConfig {

  @Bean
  public AuditorAware<java.util.UUID> auditorProvider() {
    return new AuditorAwareImpl();
  }

  @Configuration
  @EnableJpaRepositories(basePackages = "com.sun.apollo.repository", entityManagerFactoryRef = "apolloEntityManagerFactory", transactionManagerRef = "apolloTransactionManager")
  public static class ApolloJpaConfig {
  }

  @Configuration
  @EnableJpaRepositories(basePackages = "com.sun.briareus.repository", entityManagerFactoryRef = "briareusEntityManagerFactory", transactionManagerRef = "briareusTransactionManager")
  public static class BriareusJpaConfig {
  }

  @Configuration
  @EnableJpaRepositories(basePackages = "com.sun.cerberus.repository", entityManagerFactoryRef = "cerberusEntityManagerFactory", transactionManagerRef = "cerberusTransactionManager")
  public static class CerberusJpaConfig {
  }

  @Configuration
  @EnableJpaRepositories(basePackages = "com.sun.dionysus.service.repository", entityManagerFactoryRef = "dionysusEntityManagerFactory", transactionManagerRef = "dionysusTransactionManager")
  public static class DionysusJpaConfig {
  }

  @Configuration
  @EnableJpaRepositories(basePackages = "com.sun.echo.repository", entityManagerFactoryRef = "echoEntityManagerFactory", transactionManagerRef = "echoTransactionManager")
  public static class EchoJpaConfig {
  }

  @Configuration
  @EnableJpaRepositories(basePackages = "com.sun.fates.repository", entityManagerFactoryRef = "fatesEntityManagerFactory", transactionManagerRef = "fatesTransactionManager")
  public static class FatesJpaConfig {
  }

  @Configuration
  @EnableJpaRepositories(basePackages = "com.sun.gaia.repository", entityManagerFactoryRef = "gaiaEntityManagerFactory", transactionManagerRef = "gaiaTransactionManager")
  public static class GaiaJpaConfig {
  }

  @Configuration
  @EnableJpaRepositories(basePackages = "com.sun.hades.repository", entityManagerFactoryRef = "hadesEntityManagerFactory", transactionManagerRef = "hadesTransactionManager")
  public static class HadesJpaConfig {
  }

  @Configuration
  @EnableJpaRepositories(basePackages = "com.sun.icarus.repository", entityManagerFactoryRef = "icarusEntityManagerFactory", transactionManagerRef = "icarusTransactionManager")
  public static class IcarusJpaConfig {
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

  @Bean(name = "cerberusDataSource")
  @ConfigurationProperties(prefix = "spring.datasource.cerberus")
  public DataSource cerberusDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean(name = "dionysusDataSource")
  @ConfigurationProperties(prefix = "spring.datasource.dionysus")
  public DataSource dionysusDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean(name = "echoDataSource")
  @ConfigurationProperties(prefix = "spring.datasource.echo")
  public DataSource echoDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean(name = "fatesDataSource")
  @ConfigurationProperties(prefix = "spring.datasource.fates")
  public DataSource fatesDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean(name = "gaiaDataSource")
  @ConfigurationProperties(prefix = "spring.datasource.gaia")
  public DataSource gaiaDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean(name = "hadesDataSource")
  @ConfigurationProperties(prefix = "spring.datasource.hades")
  public DataSource hadesDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean(name = "icarusDataSource")
  @ConfigurationProperties(prefix = "spring.datasource.icarus")
  public DataSource icarusDataSource() {
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

  @Bean(name = "cerberusEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean cerberusEntityManagerFactory(
      @Qualifier("cerberusDataSource") DataSource dataSource) {
    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
    em.setDataSource(dataSource);
    em.setPackagesToScan("com.sun.cerberus.model");

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

  @Bean(name = "dionysusEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean dionysusEntityManagerFactory(
      @Qualifier("dionysusDataSource") DataSource dataSource) {
    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
    em.setDataSource(dataSource);
    em.setPackagesToScan("com.sun.dionysus.graphql.models");

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

  @Bean(name = "echoEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean echoEntityManagerFactory(
      @Qualifier("echoDataSource") DataSource dataSource) {
    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
    em.setDataSource(dataSource);
    em.setPackagesToScan("com.sun.echo.model");

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

  @Bean(name = "fatesEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean fatesEntityManagerFactory(
      @Qualifier("fatesDataSource") DataSource dataSource) {
    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
    em.setDataSource(dataSource);
    em.setPackagesToScan("com.sun.fates.model");

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

  @Bean(name = "gaiaEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean gaiaEntityManagerFactory(
      @Qualifier("gaiaDataSource") DataSource dataSource) {
    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
    em.setDataSource(dataSource);
    em.setPackagesToScan("com.sun.gaia.model");

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

  @Bean(name = "hadesEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean hadesEntityManagerFactory(
      @Qualifier("hadesDataSource") DataSource dataSource) {
    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
    em.setDataSource(dataSource);
    em.setPackagesToScan("com.sun.hades.model");

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

  @Bean(name = "icarusEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean icarusEntityManagerFactory(
      @Qualifier("icarusDataSource") DataSource dataSource) {
    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
    em.setDataSource(dataSource);
    em.setPackagesToScan("com.sun.icarus.model");

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

  @Bean(name = "fatesTransactionManager")
  public PlatformTransactionManager fatesTransactionManager(
      @Qualifier("fatesEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
    return new JpaTransactionManager(entityManagerFactory.getObject());
  }

  @Bean(name = "gaiaTransactionManager")
  public PlatformTransactionManager gaiaTransactionManager(
      @Qualifier("gaiaEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
    return new JpaTransactionManager(entityManagerFactory.getObject());
  }
  public PlatformTransactionManager apolloTransactionManager(
      @Qualifier("apolloEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
    return new JpaTransactionManager(entityManagerFactory.getObject());
  }

  @Bean(name = "briareusTransactionManager")
  public PlatformTransactionManager briareusTransactionManager(
      @Qualifier("briareusEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
    return new JpaTransactionManager(entityManagerFactory.getObject());
  }

  @Bean(name = "cerberusTransactionManager")
  public PlatformTransactionManager cerberusTransactionManager(
      @Qualifier("cerberusEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
    return new JpaTransactionManager(entityManagerFactory.getObject());
  }

  @Bean(name = "dionysusTransactionManager")
  public PlatformTransactionManager dionysusTransactionManager(
      @Qualifier("dionysusEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
    return new JpaTransactionManager(entityManagerFactory.getObject());
  }

  @Bean(name = "echoTransactionManager")
  public PlatformTransactionManager echoTransactionManager(
      @Qualifier("echoEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
    return new JpaTransactionManager(entityManagerFactory.getObject());
  }

  @Bean(name = "hadesTransactionManager")
  public PlatformTransactionManager hadesTransactionManager(
      @Qualifier("hadesEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
    return new JpaTransactionManager(entityManagerFactory.getObject());
  }

  @Bean(name = "icarusTransactionManager")
  public PlatformTransactionManager icarusTransactionManager(
      @Qualifier("icarusEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
    return new JpaTransactionManager(entityManagerFactory.getObject());
  }
}