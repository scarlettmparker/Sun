package com.sun.hades.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.sun.hades.codegen.types.ArchiveTextResponse;
import com.sun.hades.codegen.types.CreateSourceResponse;
import com.sun.hades.codegen.types.CreateTextResponse;
import com.sun.hades.codegen.types.MarkViewedResponse;
import com.sun.hades.codegen.types.PagedReaderTexts;
import com.sun.hades.codegen.types.PaginationInput;
import com.sun.hades.codegen.types.ReaderSource;
import com.sun.hades.codegen.types.ReaderText;
import com.sun.hades.codegen.types.ReaderTextInput;
import com.sun.hades.codegen.types.TextLevelAssessment;
import com.sun.hades.codegen.types.Word;
import com.sun.hades.codegen.types.WordDictionary;
import com.sun.hades.codegen.types.WordScope;
import com.sun.hades.graphql.services.ReaderTextGraphQLService;
import com.sun.hades.graphql.services.WordReferenceService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReaderTextDataFetcherTest {

  @Mock private ReaderTextGraphQLService readerTextGraphQLService;
  @Mock private WordReferenceService wordReferenceService;
  @Mock private DgsDataFetchingEnvironment env;

  @InjectMocks private ReaderTextDataFetcher fetcher;

  @Test
  void getHadesQueries_shouldReturnNonNull() {
    assertThat(fetcher.getHadesQueries()).isNotNull();
  }

  @Test
  void getHadesMutations_shouldReturnNonNull() {
    assertThat(fetcher.getHadesMutations()).isNotNull();
  }

  @Test
  void texts_shouldDelegateToService() {
    PaginationInput pagination = PaginationInput.newBuilder().page(0).size(10).build();
    PagedReaderTexts page = PagedReaderTexts.newBuilder().items(List.of()).build();
    when(readerTextGraphQLService.texts(pagination)).thenReturn(page);

    PagedReaderTexts result = fetcher.texts(pagination);

    assertThat(result).isEqualTo(page);
    verify(readerTextGraphQLService).texts(pagination);
  }

  @Test
  void text_shouldDelegateToService() {
    ReaderText mapped = ReaderText.newBuilder().id("id").title("T").build();
    when(readerTextGraphQLService.text("id")).thenReturn(mapped);

    ReaderText result = fetcher.text("id");

    assertThat(result).isEqualTo(mapped);
    verify(readerTextGraphQLService).text("id");
  }

  @Test
  void textContent_shouldDelegateToService() {
    ReaderText parent = ReaderText.newBuilder().id("text-id").title("T").build();
    when(env.getSource()).thenReturn(parent);
    when(readerTextGraphQLService.textContent("text-id")).thenReturn("content");

    String result = fetcher.textContent(env);

    assertThat(result).isEqualTo("content");
    verify(readerTextGraphQLService).textContent("text-id");
  }

  @Test
  void textContent_shouldReturnNullWhenNoParent() {
    when(env.getSource()).thenReturn(null);

    assertThat(fetcher.textContent(env)).isNull();
  }

  @Test
  void classifyTextLevel_shouldDelegateToService() {
    TextLevelAssessment assessment = TextLevelAssessment.newBuilder().build();
    when(readerTextGraphQLService.classifyTextLevel("hello")).thenReturn(assessment);

    TextLevelAssessment result = fetcher.classifyTextLevel("hello");

    assertThat(result).isEqualTo(assessment);
    verify(readerTextGraphQLService).classifyTextLevel("hello");
  }

  @Test
  void defineWord_shouldDelegateToService() {
    Word word = Word.newBuilder().term("hello").build();
    List<WordScope> scope = List.of(WordScope.ALL_TRANSLATIONS);
    when(wordReferenceService.defineWord("hello", scope, null)).thenReturn(word);

    Word result = fetcher.defineWord("hello", scope, null);

    assertThat(result).isEqualTo(word);
    verify(wordReferenceService).defineWord("hello", scope, null);
  }

  @Test
  void defineWord_withDictionary_shouldDelegateToService() {
    Word word = Word.newBuilder().term("hello").build();
    List<WordScope> scope = List.of(WordScope.EXAMPLES);
    when(wordReferenceService.defineWord("hello", scope, WordDictionary.ENGLISH)).thenReturn(word);

    Word result = fetcher.defineWord("hello", scope, WordDictionary.ENGLISH);

    assertThat(result).isEqualTo(word);
    verify(wordReferenceService).defineWord("hello", scope, WordDictionary.ENGLISH);
  }

  @Test
  void source_shouldDelegateToService() {
    ReaderSource source = ReaderSource.newBuilder().id("id").name("N").build();
    when(readerTextGraphQLService.source("id")).thenReturn(source);

    ReaderSource result = fetcher.source("id");

    assertThat(result).isEqualTo(source);
    verify(readerTextGraphQLService).source("id");
  }

  @Test
  void sources_shouldDelegateToService() {
    ReaderSource source = ReaderSource.newBuilder().id("id").name("N").build();
    when(readerTextGraphQLService.sources()).thenReturn(List.of(source));

    List<ReaderSource> result = fetcher.sources();

    assertThat(result).containsExactly(source);
    verify(readerTextGraphQLService).sources();
  }

  @Test
  void createSource_shouldDelegateToService() {
    CreateSourceResponse mockResult = CreateSourceResponse.newBuilder()
        .message("ok")
        .source(ReaderSource.newBuilder().id("id").name("N").url("url").build())
        .build();
    when(readerTextGraphQLService.createSource("name", "url")).thenReturn(mockResult);

    CreateSourceResponse result = fetcher.createSource("name", "url");

    assertThat(result).isEqualTo(mockResult);
    verify(readerTextGraphQLService).createSource("name", "url");
  }

  @Test
  void createText_shouldDelegateToService() {
    ReaderTextInput input = ReaderTextInput.newBuilder().title("T").content("c").language("fr").build();
    CreateTextResponse mockResult = CreateTextResponse.newBuilder()
        .message("ok")
        .text(ReaderText.newBuilder().id("id").title("T").build())
        .build();
    when(readerTextGraphQLService.createText(input)).thenReturn(mockResult);

    CreateTextResponse result = fetcher.createText(input);

    assertThat(result).isEqualTo(mockResult);
    verify(readerTextGraphQLService).createText(input);
  }

  @Test
  void archiveText_shouldDelegateToService() {
    ArchiveTextResponse mockResult = ArchiveTextResponse.newBuilder()
        .message("ok")
        .text(ReaderText.newBuilder().id("id").title("T").build())
        .build();
    when(readerTextGraphQLService.archiveText("id")).thenReturn(mockResult);

    ArchiveTextResponse result = fetcher.archiveText("id");

    assertThat(result).isEqualTo(mockResult);
    verify(readerTextGraphQLService).archiveText("id");
  }

  @Test
  void markViewed_shouldDelegateToService() {
    MarkViewedResponse mockResult = MarkViewedResponse.newBuilder()
        .message("ok")
        .textId("id")
        .build();
    when(readerTextGraphQLService.markViewed("id")).thenReturn(mockResult);

    MarkViewedResponse result = fetcher.markViewed("id");

    assertThat(result).isEqualTo(mockResult);
    verify(readerTextGraphQLService).markViewed("id");
  }
}
