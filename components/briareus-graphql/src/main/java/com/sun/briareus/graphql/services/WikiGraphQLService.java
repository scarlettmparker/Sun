package com.sun.briareus.graphql.services;

import com.sun.base.nlp.WikipediaService;
import com.sun.base.nlp.WikipediaSummary;
import com.sun.base.nlp.WiktionaryEntry;
import com.sun.base.nlp.WiktionaryService;
import com.sun.briareus.codegen.types.Entry;
import com.sun.briareus.codegen.types.Summary;
import com.sun.briareus.graphql.mappers.EntryMapper;
import com.sun.briareus.graphql.mappers.SummaryMapper;
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
}
