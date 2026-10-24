package com.sun.dionysus.subtitle;

import com.sun.dionysus.model.enums.SubtitleSource;

/**
 * A subtitle search result returned by OpenSubtitles.
 */
public final class SubtitleSearchResult {

  private final SubtitleSource source;
  private final String subtitleKey;
  private final Integer fileId;
  private final String language;
  private final String label;
  private final Boolean hearingImpaired;
  private final Integer downloads;
  private final Double rating;

  /**
   * Creates a search result.
   */
  public SubtitleSearchResult(
      SubtitleSource source,
      String subtitleKey,
      Integer fileId,
      String language,
      String label,
      Boolean hearingImpaired,
      Integer downloads,
      Double rating) {
    this.source = source;
    this.subtitleKey = subtitleKey;
    this.fileId = fileId;
    this.language = language;
    this.label = label;
    this.hearingImpaired = hearingImpaired;
    this.downloads = downloads;
    this.rating = rating;
  }

  public SubtitleSource getSource() {
    return source;
  }

  public String getSubtitleKey() {
    return subtitleKey;
  }

  public Integer getFileId() {
    return fileId;
  }

  public String getLanguage() {
    return language;
  }

  public String getLabel() {
    return label;
  }

  public Boolean getHearingImpaired() {
    return hearingImpaired;
  }

  public Integer getDownloads() {
    return downloads;
  }

  public Double getRating() {
    return rating;
  }

  // Record-style accessors for compatibility.
  public SubtitleSource source() {
    return source;
  }

  public String subtitleKey() {
    return subtitleKey;
  }

  public Integer fileId() {
    return fileId;
  }

  public String language() {
    return language;
  }

  public String label() {
    return label;
  }

  public Boolean hearingImpaired() {
    return hearingImpaired;
  }

  public Integer downloads() {
    return downloads;
  }

  public Double rating() {
    return rating;
  }
}
