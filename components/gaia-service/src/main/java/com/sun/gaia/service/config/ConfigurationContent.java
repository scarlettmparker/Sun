package com.sun.gaia.service.config;

import java.util.List;
import java.util.Map;

/**
 * Parsed view of a configuration's desired-state document.
 */
public record ConfigurationContent(
    List<User> users,
    List<Schema> schemas,
    List<PropertySet> propertySets) {

  /** A ghost account to ensure exists. */
  public record User(String key, String accountType) {
  }

  /** A property-set schema to register. */
  public record Schema(String ownerKey, String name, boolean configurable,
      Map<String, Object> properties) {
  }

  /** A property set whose entries should be materialised. */
  public record PropertySet(String ownerKey, String name, boolean configurable,
      List<Entry> entries) {
  }

  /** A single entry within a property set. */
  public record Entry(String name, Map<String, Object> values) {
  }
}
