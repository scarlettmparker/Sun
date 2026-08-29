package com.sun.base.nlp;

public record WikipediaSummary(
    /**
     * Canonical title.
     */
    String title,
    /**
     * Summary extract.
     */
    String extract,
    /**
     * Desktop page url.
     */
    String pageUrl,
    /**
     * Thumbnail url if present.
     */
    String thumbnailUrl) {
}
