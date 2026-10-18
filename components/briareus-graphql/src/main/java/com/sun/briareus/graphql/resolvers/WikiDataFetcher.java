package com.sun.briareus.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.base.nlp.WikipediaRelatedTopic;
import com.sun.base.nlp.WikipediaService;
import com.sun.base.nlp.WikipediaSummary;
import com.sun.base.nlp.WiktionaryEntry;
import com.sun.base.nlp.WiktionaryService;
import com.sun.briareus.codegen.types.Entry;
import com.sun.briareus.codegen.types.RelatedTopic;
import com.sun.briareus.codegen.types.Summary;
import com.sun.briareus.codegen.types.WikiQueries;
import com.sun.briareus.graphql.mappers.EntryMapper;
import com.sun.briareus.graphql.mappers.SummaryMapper;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

@DgsComponent
public class WikiDataFetcher {

  private final WikipediaService wikipediaService;
  private final WiktionaryService wiktionaryService;
  private final SummaryMapper summaryMapper;
  private final EntryMapper entryMapper;

  public WikiDataFetcher(WikipediaService wikipediaService, WiktionaryService wiktionaryService,
      SummaryMapper summaryMapper, EntryMapper entryMapper) {
    this.wikipediaService = wikipediaService;
    this.wiktionaryService = wiktionaryService;
    this.summaryMapper = summaryMapper;
    this.entryMapper = entryMapper;
  }

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
  @Transactional(readOnly = true)
  public Summary wikipediaSummary(String title) {
    WikipediaSummary domain = wikipediaService.summary(title);
    return summaryMapper.map(domain);
  }

  /**
   * Fetches a Wiktionary entry.
   *
   * @param word the headword
   * @return the entry or null
   */
  @DgsData(parentType = "WikiQueries", field = "wiktionaryEntry")
  @PreAuthorize("@permissions.has('graphql.briareus.wiktionaryEntry')")
  @Transactional(readOnly = true)
  public Entry wiktionaryEntry(String word) {
    WiktionaryEntry domain = wiktionaryService.define(word);
    return entryMapper.map(domain);
  }

  /**
   * Searches Wikipedia for closest matches.
   *
   * @param query the search query
   * @return the summaries for matches
   */
  @DgsData(parentType = "WikiQueries", field = "wikipediaSearch")
  @PreAuthorize("@permissions.has('graphql.briareus.wikipediaSearch')")
  @Transactional(readOnly = true)
  public List<Summary> wikipediaSearch(String query) {
    List<WikipediaSummary> domain = wikipediaService.search(query);
    return domain.stream().map(summaryMapper::map).filter(s -> s != null).toList();
  }

  /**
   * Fetches full plaintext for a page.
   *
   * @param title the page title
   * @return the plaintext or null
   */
  @DgsData(parentType = "WikiQueries", field = "wikipediaPage")
  @PreAuthorize("@permissions.has('graphql.briareus.wikipediaPage')")
  @Transactional(readOnly = true)
  public String wikipediaPage(String title) {
    return wikipediaService.page(title);
  }

  /**
   * Fetches related Wikipedia topics.
   *
   * @param title the page title
   * @return the related topics
   */
  @DgsData(parentType = "WikiQueries", field = "wikipediaRelatedTopics")
  @PreAuthorize("@permissions.has('graphql.briareus.wikipediaRelatedTopics')")
  @Transactional(readOnly = true)
  public List<RelatedTopic> wikipediaRelatedTopics(String title) {
    List<WikipediaRelatedTopic> domain = wikipediaService.relatedTopics(title);
    return domain.stream()
        .map(r -> RelatedTopic.newBuilder()
            .title(r.title())
            .pageUrl(r.pageUrl())
            .extract(r.extract())
            .build())
        .toList();
  }
}
