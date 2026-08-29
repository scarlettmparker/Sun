package com.sun.briareus.graphql.mappers;

import com.sun.base.nlp.WiktionaryEntry;
import com.sun.briareus.codegen.types.Entry;
import org.springframework.stereotype.Component;

/**
 * Converts Wiktionary entries to GraphQL.
 */
@Component
public class EntryMapper {

  /**
   * Maps a domain entry to GraphQL.
   *
   * @param domain the domain entry
   * @return the GraphQL entry or null
   */
  public Entry map(WiktionaryEntry domain) {
    if (domain == null) {
      return null;
    }
    return Entry.newBuilder()
        .word(domain.word())
        .definitions(domain.definitions())
        .sourceUrl(domain.sourceUrl())
        .build();
  }
}
