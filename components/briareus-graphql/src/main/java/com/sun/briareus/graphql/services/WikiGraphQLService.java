package com.sun.briareus.graphql.services;

import com.sun.base.nlp.WikipediaRelatedTopic;
import com.sun.base.nlp.WikipediaService;
import com.sun.base.nlp.WikipediaSummary;
import com.sun.base.nlp.WiktionaryEntry;
import com.sun.base.nlp.WiktionaryService;
import com.sun.briareus.codegen.types.Entry;
import com.sun.briareus.codegen.types.RelatedTopic;
import com.sun.briareus.codegen.types.Summary;
import com.sun.briareus.graphql.mappers.EntryMapper;
import com.sun.briareus.graphql.mappers.SummaryMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL logic for wiki lookups.
 */
@Service
public class WikiGraphQLService {

  @Autowired
  private WikipediaService wikipediaService;

  @Autowired
  private WiktionaryService wiktionaryService;

  @Autowired
  private SummaryMapper summaryMapper;

  @Autowired
  private EntryMapper entryMapper;

  /**
   * Fetches a Wikipedia summary.
   *
   * @param title the page title
   * @return the summary or null
   */
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
  @Transactional(readOnly = true)
  public Entry wiktionaryEntry(String word) {
    WiktionaryEntry domain = wiktionaryService.define(word);
    return entryMapper.map(domain);
  }

  /**
   * Fetches related Wikipedia topics.
   *
   * @param title the page title
   * @return the related topics
   */
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

  /**
   * Fetches full plaintext for a page.
   *
   * @param title the page title
   * @return the plaintext or null
   */
  @Transactional(readOnly = true)
  public String wikipediaPage(String title) {
    return wikipediaService.page(title);
  }

  /**
   * Searches Wikipedia for closest matches.
   *
   * @param query the search query
   * @return the summaries for matches
   */
  @Transactional(readOnly = true)
  public List<Summary> wikipediaSearch(String query) {
    List<WikipediaSummary> domain = wikipediaService.search(query);
    return domain.stream().map(summaryMapper::map).filter(s -> s != null).toList();
  }
}
