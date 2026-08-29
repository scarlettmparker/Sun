package com.sun.hades.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.sun.hades.codegen.types.HadesMutations;
import com.sun.hades.codegen.types.HadesQueries;
import com.sun.hades.codegen.types.PagedReaderTexts;
import com.sun.hades.codegen.types.PagedTextViews;
import com.sun.hades.codegen.types.PaginationInput;
import com.sun.hades.codegen.types.QueryResult;
import com.sun.hades.codegen.types.ReaderSource;
import com.sun.hades.codegen.types.ReaderText;
import com.sun.hades.codegen.types.ReaderTextInput;
import com.sun.hades.codegen.types.TextLevelAssessment;
import com.sun.hades.codegen.types.Word;
import com.sun.hades.codegen.types.WordScope;
import com.sun.hades.graphql.services.ReaderTextGraphQLService;
import com.sun.hades.graphql.services.WordReferenceService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Data fetchers for the reader queries and mutations.
 */
@DgsComponent
public class ReaderTextDataFetcher {

  private final ReaderTextGraphQLService readerTextGraphQLService;
  private final WordReferenceService wordReferenceService;

  public ReaderTextDataFetcher(ReaderTextGraphQLService readerTextGraphQLService,
      WordReferenceService wordReferenceService) {
    this.readerTextGraphQLService = readerTextGraphQLService;
    this.wordReferenceService = wordReferenceService;
  }

  /**
   * Provides the reader queries object.
   *
   * @return a new HadesQueries instance
   */
  @DgsData(parentType = "Query", field = "hadesQueries")
  public HadesQueries getHadesQueries() {
    return HadesQueries.newBuilder().build();
  }

  /**
   * Provides the reader mutations object.
   *
   * @return a new HadesMutations instance
   */
  @DgsData(parentType = "Mutation", field = "hadesMutations")
  public HadesMutations getHadesMutations() {
    return HadesMutations.newBuilder().build();
  }

  /**
   * Lists texts, optionally filtered.
   *
   * @param level optional CEFR level
   * @param sourceId optional source id
   * @param ownerId optional owner account id
   * @param pagination the pagination input
   * @return a page of texts
   */
  @DgsData(parentType = "HadesQueries", field = "texts")
  @PreAuthorize("@permissions.has('graphql.hades.texts')")
  public PagedReaderTexts texts(PaginationInput pagination) {
    return readerTextGraphQLService.texts(pagination);
  }

  /**
   * Locates a text by id.
   *
   * @param id the text id
   * @return the text
   */
  @DgsData(parentType = "HadesQueries", field = "text")
  @PreAuthorize("@permissions.has('graphql.hades.text')")
  public ReaderText text(String id) {
    return readerTextGraphQLService.text(id);
  }

  /**
   * Resolves the heavy text body only when the client selects it, so list
   * queries that ask for id/title don't pull and serialize the full content.
   *
   * @param env the data-fetching environment, providing the parent ReaderText
   * @return the text content, or null when the parent has no id
   */
  @DgsData(parentType = "ReaderText", field = "content")
  @PreAuthorize("@permissions.has('graphql.hades.textContent')")
  public String textContent(DgsDataFetchingEnvironment env) {
    ReaderText parent = env.getSource();
    if (parent == null || parent.getId() == null) {
      return null;
    }
    return readerTextGraphQLService.textContent(parent.getId());
  }

  /**
   * Predicts the CEFR level of a text.
   *
   * @param text the text to classify
   * @return the assessment
   */
  @DgsData(parentType = "HadesQueries", field = "classifyTextLevel")
  @PreAuthorize("@permissions.has('graphql.hades.classifyTextLevel')")
  public TextLevelAssessment classifyTextLevel(String text) {
    return readerTextGraphQLService.classifyTextLevel(text);
  }

  /**
   * Defines a word from WordReference, honoring the requested scopes.
   *
   * @param word the headword to look up
   * @param scope the parts of the page to include
   * @return the word, or null when the entry does not exist
   */
  @DgsData(parentType = "HadesQueries", field = "defineWord")
  @PreAuthorize("@permissions.has('graphql.hades.defineWord')")
  public Word defineWord(String word, List<WordScope> scope) {
    return wordReferenceService.defineWord(word, scope);
  }

  /**
   * Locates a source by id.
   *
   * @param id the source id
   * @return the source
   */
  @DgsData(parentType = "HadesQueries", field = "source")
  @PreAuthorize("@permissions.has('graphql.hades.source')")
  public ReaderSource source(String id) {
    return readerTextGraphQLService.source(id);
  }

  /**
   * Lists all sources.
   *
   * @return the sources
   */
  @DgsData(parentType = "HadesQueries", field = "sources")
  @PreAuthorize("@permissions.has('graphql.hades.sources')")
  public List<ReaderSource> sources() {
    return readerTextGraphQLService.sources();
  }

  /**
   * Creates a source.
   *
   * @param name the source name
   * @param url the source url
   * @return a QueryResult
   */
  @DgsData(parentType = "HadesMutations", field = "createSource")
  @PreAuthorize("@permissions.has('graphql.hades.createSource')")
  public QueryResult createSource(String name, String url) {
    return readerTextGraphQLService.createSource(name, url);
  }

  /**
   * Creates a text.
   *
   * @param input the text input
   * @return a QueryResult
   */
  @DgsData(parentType = "HadesMutations", field = "createText")
  @PreAuthorize("@permissions.has('graphql.hades.createText')")
  public QueryResult createText(ReaderTextInput input) {
    return readerTextGraphQLService.createText(input);
  }

  /**
   * Archives a text.
   *
   * @param id the text id
   * @return a QueryResult
   */
  @DgsData(parentType = "HadesMutations", field = "archiveText")
  @PreAuthorize("@permissions.has('graphql.hades.archiveText')")
  public QueryResult archiveText(String id) {
    return readerTextGraphQLService.archiveText(id);
  }

  /**
   * Marks a text as viewed.
   *
   * @param textId the text id
   * @return a QueryResult
   */
  @DgsData(parentType = "HadesMutations", field = "markViewed")
  @PreAuthorize("@permissions.has('graphql.hades.markViewed')")
  public QueryResult markViewed(String textId) {
    return readerTextGraphQLService.markViewed(textId);
  }

  /**
   * Lists viewed texts.
   *
   * @param pagination the pagination
   * @return a page of views
   */
  @DgsData(parentType = "HadesQueries", field = "viewedTexts")
  @PreAuthorize("@permissions.has('graphql.hades.viewedTexts')")
  public PagedTextViews viewedTexts(PaginationInput pagination) {
    return readerTextGraphQLService.viewedTexts(pagination);
  }

  /**
   * Lists the full texts the caller has viewed, in viewed-at order.
   *
   * @param pagination the pagination
   * @return a page of texts
   */
  @DgsData(parentType = "HadesQueries", field = "viewedReaderTexts")
  @PreAuthorize("@permissions.has('graphql.hades.viewedReaderTexts')")
  public PagedReaderTexts viewedReaderTexts(PaginationInput pagination) {
    return readerTextGraphQLService.viewedReaderTexts(pagination);
  }

  /**
   * Locates reader texts by ids for batch attach resolution.
   *
   * @param ids the text ids
   * @return the texts
   */
  @DgsData(parentType = "HadesQueries", field = "locateReaderTexts")
  @PreAuthorize("@permissions.has('graphql.hades.locateReaderTexts')")
  public List<ReaderText> locateReaderTexts(List<String> ids) {
    return readerTextGraphQLService.locateReaderTexts(ids);
  }
}
