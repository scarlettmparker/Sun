package com.sun.hades.service;

import com.sun.hades.model.enums.CefrLevel;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The fixed learner levels granted by Discord guild roles, keyed for storage
 * and translation. Role ids are read from the member's own token; this maps
 * them to a stable key, a display name, and a CEFR equivalent.
 */
public final class ReaderLevels {

  /**
   * A learner level.
   *
   * @param key the stable key (storage + i18n)
   * @param name the display name
   * @param cefrLevel the CEFR equivalent, or null when none applies
   */
  public record Level(String key, String name, CefrLevel cefrLevel) {
  }

  /** Every known level. */
  public static final List<Level> ALL = List.of(
      new Level("non-learner", "Non Learner", null),
      new Level("native", "Native", CefrLevel.C2),
      new Level("beginner", "Beginner", CefrLevel.A1),
      new Level("elementary", "Elementary", CefrLevel.A2),
      new Level("intermediate", "Intermediate", CefrLevel.B1),
      new Level("upper-intermediate", "Upper Intermediate", CefrLevel.B2),
      new Level("advanced", "Advanced", CefrLevel.C1),
      new Level("fluent", "Fluent", CefrLevel.C2));

  /** Levels keyed by their stable key. */
  public static final Map<String, Level> BY_KEY = ALL.stream()
      .collect(Collectors.toUnmodifiableMap(Level::key, level -> level));

  /** Discord guild role id to level key. */
  public static final Map<String, String> ID_TO_KEY = Map.of(
      "352001527780474881", "non-learner",
      "350483752490631181", "native",
      "351117824300679169", "beginner",
      "351117954974482435", "elementary",
      "350485376109903882", "intermediate",
      "351118486426091521", "upper-intermediate",
      "350485279238258689", "advanced",
      "350483489461895168", "fluent");

  private ReaderLevels() {
  }
}
