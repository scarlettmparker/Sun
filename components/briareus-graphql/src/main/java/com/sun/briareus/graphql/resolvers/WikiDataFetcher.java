package com.sun.briareus.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.briareus.codegen.types.Entry;
import com.sun.briareus.codegen.types.Summary;
import com.sun.briareus.codegen.types.WikiQueries;
import com.sun.briareus.graphql.services.WikiGraphQLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

@DgsComponent
public class WikiDataFetcher {

  @Autowired
  private WikiGraphQLService wikiGraphQLService;

  /**
   * Provides the wiki queries object.
   *
   * @return a new WikiQueries instance
   */
  @DgsData(parentType = "Query", field = "wikiQueries")
  public WikiQueries getWikiQueries() {
    return WikiQueries.newBuilder().build();
  }

  /**
   * Fetches a Wikipedia summary.
   *
   * @param title the page title
   * @return the summary or null
   */
  @DgsData(parentType = "WikiQueries", field = "wikipediaSummary")
  @PreAuthorize("@permissions.has('graphql.briareus.wikipediaSummary')")
  public Summary wikipediaSummary(String title) {
    return wikiGraphQLService.wikipediaSummary(title);
  }

  /**
   * Fetches a Wiktionary entry.
   *
   * @param word the headword
   * @return the entry or null
   */
  @DgsData(parentType = "WikiQueries", field = "wiktionaryEntry")
  @PreAuthorize("@permissions.has('graphql.briareus.wiktionaryEntry')")
  public Entry wiktionaryEntry(String word) {
    return wikiGraphQLService.wiktionaryEntry(word);
  }
}
