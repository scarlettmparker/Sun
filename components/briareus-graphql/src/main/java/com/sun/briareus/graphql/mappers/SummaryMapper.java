package com.sun.briareus.graphql.mappers;

import com.sun.base.nlp.WikipediaSummary;
import com.sun.briareus.codegen.types.Summary;
import org.springframework.stereotype.Component;

/**
 * Converts Wikipedia summaries to GraphQL.
 */
@Component
public class SummaryMapper {

  /**
   * Maps a domain summary to GraphQL.
   *
   * @param domain the domain summary
   * @return the GraphQL summary or null
   */
  public Summary map(WikipediaSummary domain) {
    if (domain == null) {
      return null;
    }
    return Summary.newBuilder()
        .title(domain.title())
        .extract(domain.extract())
        .pageUrl(domain.pageUrl())
        .thumbnailUrl(domain.thumbnailUrl())
        .build();
  }
}
