package com.sun.gaia.service;

import com.sun.gaia.model.ConfigurationEntity;
import com.sun.gaia.repository.ConfigurationRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seeds the default ReactApp themes configuration on first boot.
 */
@Component
@Profile("!test")
public class ThemeConfigurationSeeder implements ApplicationRunner {

  private static final Logger logger = LoggerFactory.getLogger(ThemeConfigurationSeeder.class);

  private final ConfigurationRepository configurationRepository;
  private final ConfigurationReconciler reconciler;

  public ThemeConfigurationSeeder(ConfigurationRepository configurationRepository,
      ConfigurationReconciler reconciler) {
    this.configurationRepository = configurationRepository;
    this.reconciler = reconciler;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!configurationRepository.findAll().isEmpty()) {
      return;
    }
    logger.info("Seeding default ReactApp themes configuration");
    ConfigurationEntity config = new ConfigurationEntity();
    config.setName("react-app-themes");
    config.setDescription("Default themes for the ReactApp ghost account");
    config.setEnabled(true);
    config.setContent(buildContent());
    configurationRepository.save(config);
    reconciler.reconcile(config);
  }

  /**
   * Builds the desired-state document for the themes configuration.
   *
   * @return the content map
   */
  private Map<String, Object> buildContent() {
    Map<String, Object> content = new LinkedHashMap<>();
    content.put("users", List.of(user("ReactApp", "GHOST")));
    content.put("schemas", List.of(schema("ReactApp", "themes", true, themeSchema())));
    content.put("propertySets", List.of(
        propertySet("ReactApp", "themes", true, List.of(
            entry("default", defaultTheme()),
            entry("greek", greekTheme())))));
    return content;
  }

  /**
   * Builds the themes schema property definitions.
   *
   * @return the property definitions
   */
  private Map<String, Object> themeSchema() {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("primary", colour(true));
    properties.put("primary-hover", colour(false));
    properties.put("primary-active", colour(false));
    properties.put("secondary", colour(true));
    properties.put("secondary-hover", colour(false));
    properties.put("accent", colour(true));
    properties.put("accent-hover", colour(false));
    properties.put("tertiary", colour(true));
    properties.put("tertiary-hover", colour(false));
    return properties;
  }

  private Map<String, Object> defaultTheme() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("primary", "#d90429");
    values.put("primary-hover", "#fb3758");
    values.put("primary-active", "#a0031d");
    values.put("secondary", "#ffffff");
    values.put("secondary-hover", "#f5f5f5");
    values.put("accent", "#ffdaad");
    values.put("accent-hover", "#ffe3c2");
    values.put("tertiary", "#d03991");
    values.put("tertiary-hover", "#dc6aad");
    return values;
  }

  private Map<String, Object> greekTheme() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("primary", "#1d4ed8");
    values.put("primary-hover", "#3b82f6");
    values.put("primary-active", "#1e3a8a");
    values.put("secondary", "#ffffff");
    values.put("secondary-hover", "#f5f5f5");
    values.put("accent", "#bfdbfe");
    values.put("accent-hover", "#dbeafe");
    values.put("tertiary", "#6d28d9");
    values.put("tertiary-hover", "#7c3aed");
    return values;
  }

  private Map<String, Object> colour(boolean required) {
    Map<String, Object> definition = new LinkedHashMap<>();
    definition.put("type", "color");
    definition.put("required", required);
    return definition;
  }

  private Map<String, Object> user(String key, String accountType) {
    Map<String, Object> user = new LinkedHashMap<>();
    user.put("key", key);
    user.put("accountType", accountType);
    return user;
  }

  private Map<String, Object> schema(String ownerKey, String name, boolean configurable,
      Map<String, Object> properties) {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("ownerKey", ownerKey);
    schema.put("name", name);
    schema.put("configurable", configurable);
    schema.put("properties", properties);
    return schema;
  }

  private Map<String, Object> propertySet(String ownerKey, String name, boolean configurable,
      List<Map<String, Object>> entries) {
    Map<String, Object> propertySet = new LinkedHashMap<>();
    propertySet.put("ownerKey", ownerKey);
    propertySet.put("name", name);
    propertySet.put("configurable", configurable);
    propertySet.put("entries", entries);
    return propertySet;
  }

  private Map<String, Object> entry(String name, Map<String, Object> values) {
    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put("name", name);
    entry.put("values", values);
    return entry;
  }
}
