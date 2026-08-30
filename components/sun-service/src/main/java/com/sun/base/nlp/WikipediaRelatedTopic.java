package com.sun.base.nlp;

public record WikipediaRelatedTopic(
    /**
     * Title of the related page.
     */
    String title,
    /**
     * URL of the related page.
     */
    String pageUrl,
    /**
     * Short extract for the page.
     */
    String extract) {}
