package com.sun.gaia.graphql.services;

import com.sun.gaia.codegen.types.HubApp;
import com.sun.gaia.codegen.types.HubAppInput;
import com.sun.gaia.codegen.types.HubMode;
import com.sun.gaia.codegen.types.HubRegistry;
import com.sun.gaia.codegen.types.HubRegistryInput;
import com.sun.gaia.model.PropertySetEntryEntity;
import com.sun.gaia.service.PropertySetService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL business logic for the hub registry.
 */
@Service
public class HubRegistryGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(HubRegistryGraphQLService.class);

  /**
   * Property set backing the hub registry.
   */
  private static final String HUB_OWNER_KEY = "hub";
  private static final String HUB_SET_NAME = "registry";
  private static final String HUB_ENTRY_NAME = "apps";

  private final PropertySetService propertySetService;

  public HubRegistryGraphQLService(PropertySetService propertySetService) {
    this.propertySetService = propertySetService;
  }

  /**
   * Returns the stored hub registry, or a default when none is present.
   *
   * @return the hub registry
   */
  @Transactional(readOnly = true)
  public HubRegistry hubRegistry() {
    return propertySetService.getEntry(HUB_OWNER_KEY, HUB_SET_NAME, HUB_ENTRY_NAME)
        .map(PropertySetEntryEntity::getValues)
        .map(this::toHubRegistry)
        .orElseGet(this::defaultHubRegistry);
  }

  /**
   * Validates and persists the hub registry.
   *
   * @param input the hub registry input
   * @return the saved hub registry
   */
  @Transactional
  public HubRegistry saveRegistry(HubRegistryInput input) {
    if (input.getMode() == null) {
      throw new IllegalArgumentException("Hub mode is required");
    }
    List<Map<String, Object>> apps = new ArrayList<>();
    for (HubAppInput app : input.getApps()) {
      if (app.getKey() == null || app.getKey().isBlank()) {
        throw new IllegalArgumentException("Hub app key is required");
      }
      if (app.getDevPort() <= 0 || app.getProdPort() <= 0) {
        throw new IllegalArgumentException("Hub app ports must be positive");
      }
      apps.add(toAppValues(app));
    }
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("mode", input.getMode().name());
    values.put("apps", apps);
    propertySetService.upsertEntry(HUB_OWNER_KEY, HUB_SET_NAME, HUB_ENTRY_NAME, values, false);
    return toHubRegistry(values);
  }

  /**
   * Builds a default registry mirroring the node app's seed.
   *
   * @return the default registry
   */
  private HubRegistry defaultHubRegistry() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("mode", "dev");
    values.put("apps", List.of(
        appValues("sun", "Sun", ".", 5173, 5173,
            "https://sun.int.scarlettparker.co.uk", "Ecosystem home with blog and gallery", true, true),
        appValues("guided-reader", "Guided Reader", "../Guided-Reader", 5178, 5178,
            "http://localhost:5178", "Reading app with texts, annotations and a forum", true, false),
        appValues("checklist", "Checklist", "../Checklist", 5176, 5176,
            "https://checklist.int.scarlettparker.co.uk", "Structured checklists", true, false),
        appValues("viewer", "Viewer", "../Viewer", 5177, 5177,
            "https://viewer.int.scarlettparker.co.uk", "Interactive viewer", true, false),
        appValues("mame", "Emulator", "../MAME", 5175, 5180,
            "https://emulator.int.scarlettparker.co.uk", "Browser MAME emulator", true, false)));
    return toHubRegistry(values);
  }

  /**
   * Maps stored registry values into the typed registry.
   *
   * @param values the stored values
   * @return the typed registry
   */
  private HubRegistry toHubRegistry(Map<String, Object> values) {
    HubRegistry.Builder builder = HubRegistry.newBuilder();
    builder.mode(fromHubMode(String.valueOf(values.getOrDefault("mode", "dev"))));
    List<HubApp> apps = new ArrayList<>();
    if (values.get("apps") instanceof List<?> list) {
      for (Object item : list) {
        if (item instanceof Map<?, ?> appMap) {
          HubApp app = toHubApp(appMap);
          if (app != null) {
            apps.add(app);
          }
        }
      }
    }
    return builder.apps(apps).build();
  }

  /**
   * Maps a stored app map into the typed app, or null when the key is absent.
   *
   * @param map the stored app map
   * @return the typed app
   */
  private HubApp toHubApp(Map<?, ?> map) {
    Object key = map.get("key");
    if (key == null || String.valueOf(key).isBlank()) {
      return null;
    }
    String appKey = String.valueOf(key);
    HubApp.Builder builder = HubApp.newBuilder();
    builder.key(appKey);
    builder.name(strValue(map.get("name"), appKey));
    builder.dir(strValue(map.get("dir"), "../" + appKey));
    builder.devPort(intValue(map.get("devPort"), 5173));
    builder.prodPort(intValue(map.get("prodPort"), 5173));
    builder.url(strValue(map.get("url"), ""));
    builder.description(strValue(map.get("description"), ""));
    builder.enabled(!Boolean.FALSE.equals(map.get("enabled")));
    builder.self(Boolean.TRUE.equals(map.get("self")));
    return builder.build();
  }

  /**
   * Serialises a typed app input into a stored values map.
   *
   * @param app the app input
   * @return the stored app map
   */
  private Map<String, Object> toAppValues(HubAppInput app) {
    return appValues(app.getKey(), app.getName(), app.getDir(), app.getDevPort(),
        app.getProdPort(), app.getUrl(), app.getDescription(),
        app.getEnabled(), Boolean.TRUE.equals(app.getSelf()));
  }

  /**
   * Builds a stored app values map.
   *
   * @param key the app key
   * @param name the display name
   * @param dir the repo path
   * @param devPort the dev port
   * @param prodPort the prod port
   * @param url the public url
   * @param description the description
   * @param enabled whether the app is managed
   * @param self whether this is the current app
   * @return the stored app map
   */
  private Map<String, Object> appValues(String key, String name, String dir, int devPort,
      int prodPort, String url, String description, boolean enabled, boolean self) {
    Map<String, Object> app = new LinkedHashMap<>();
    app.put("key", key);
    app.put("name", name);
    app.put("dir", dir);
    app.put("devPort", devPort);
    app.put("prodPort", prodPort);
    app.put("url", url);
    app.put("description", description);
    app.put("enabled", enabled);
    app.put("self", self);
    return app;
  }

  /**
   * Resolves a stored mode string into the typed mode, defaulting to dev.
   *
   * @param value the stored mode
   * @return the typed mode
   */
  private HubMode fromHubMode(String value) {
    try {
      return HubMode.valueOf(value);
    } catch (IllegalArgumentException e) {
      return HubMode.dev;
    }
  }

  /**
   * Reads a string value with a fallback.
   *
   * @param value the stored value
   * @param fallback the fallback string
   * @return the string value
   */
  private String strValue(Object value, String fallback) {
    return value instanceof String s && !s.isBlank() ? s : fallback;
  }

  /**
   * Reads an integer value with a fallback.
   *
   * @param value the stored value
   * @param fallback the fallback integer
   * @return the integer value
   */
  private int intValue(Object value, int fallback) {
    return value instanceof Number n && n.intValue() > 0 ? n.intValue() : fallback;
  }
}
