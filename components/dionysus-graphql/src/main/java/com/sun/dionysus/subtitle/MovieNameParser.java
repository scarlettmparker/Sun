package com.sun.dionysus.subtitle;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a movie file name into a query and year.
 */
public final class MovieNameParser {

  private static final Pattern YEAR_PATTERN = Pattern.compile("(?<!\\d)((?:19|20)\\d{2})(?!\\d)");

  private MovieNameParser() {
  }

  /**
   * Parses a movie file name.
   *
   * @param fileName the file name or key
   * @return the parsed query and year
   */
  public static ParsedMovieName parse(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      return new ParsedMovieName("", null);
    }

    String name = fileName.trim();
    if (name.toLowerCase(Locale.ROOT).endsWith(".mp4")) {
      name = name.substring(0, name.length() - 4);
    }

    int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
    if (slash >= 0) {
      name = name.substring(slash + 1);
    }

    int dot = name.lastIndexOf('.');
    if (dot > 0) {
      name = name.substring(0, dot);
    }

    name = name.replace('_', ' ').replace('.', ' ');
    name = name.replaceAll("\\s+", " ").trim();

    Matcher matcher = YEAR_PATTERN.matcher(name);
    if (!matcher.find()) {
      return new ParsedMovieName(name, null);
    }

    int yearIndex = matcher.start();
    Integer year = Integer.valueOf(matcher.group(1));
    String query = name.substring(0, yearIndex).trim();
    query = query.replaceAll("[\\s()\\[\\]-]+$", "").trim();
    query = query.replaceAll("\\s+", " ");
    if (query.isBlank()) {
      query = name;
    }
    return new ParsedMovieName(query, year);
  }

  /**
   * Parses a movie file name.
   *
   * @param fileName the file name or key
   * @return the parsed query and year
   */
  public static ParsedMovieName parseName(String fileName) {
    return parse(fileName);
  }

  /**
   * Holds a parsed movie name.
   */
  public static final class ParsedMovieName {

    private final String query;
    private final Integer year;

    /**
     * Creates a parsed movie name.
     *
     * @param query the search query
     * @param year the release year
     */
    public ParsedMovieName(String query, Integer year) {
      this.query = query == null ? "" : query;
      this.year = year;
    }

    /**
     * Returns the search query.
     *
     * @return the search query
     */
    public String getQuery() {
      return query;
    }

    /**
     * Returns the search query.
     *
     * @return the search query
     */
    public String query() {
      return query;
    }

    /**
     * Returns the search query.
     *
     * @return the search query
     */
    public String title() {
      return query;
    }

    /**
     * Returns the release year.
     *
     * @return the release year
     */
    public Integer getYear() {
      return year;
    }

    /**
     * Returns the release year.
     *
     * @return the release year
     */
    public Integer year() {
      return year;
    }
  }
}
