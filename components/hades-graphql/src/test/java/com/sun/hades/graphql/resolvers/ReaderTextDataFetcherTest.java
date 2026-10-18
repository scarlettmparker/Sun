package com.sun.hades.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.sun.gaia.service.UserContextHolder;
import com.sun.hades.codegen.types.ArchiveTextResponse;
import com.sun.hades.codegen.types.CreateSourceResponse;
import com.sun.hades.codegen.types.CreateTextResponse;
import com.sun.hades.codegen.types.MarkViewedResponse;
import com.sun.hades.codegen.types.PagedReaderTexts;
import com.sun.hades.codegen.types.ReaderSource;
import com.sun.hades.codegen.types.ReaderText;
import com.sun.hades.codegen.types.ReaderTextInput;
import com.sun.hades.codegen.types.TextLevelAssessment;
import com.sun.hades.codegen.types.Word;
import com.sun.hades.codegen.types.WordDictionary;
import com.sun.hades.codegen.types.WordScope;
import com.sun.hades.graphql.inference.InferenceClient;
import com.sun.hades.graphql.mappers.ReaderSourceMapper;
import com.sun.hades.graphql.mappers.ReaderTextMapper;
import com.sun.hades.graphql.services.WordReferenceService;
import com.sun.hades.model.ReaderSourceEntity;
import com.sun.hades.model.ReaderTextEntity;
import com.sun.hades.model.enums.CefrLevel;
import com.sun.hades.model.enums.ReaderTextStatus;
import com.sun.hades.service.ReaderSourceService;
import com.sun.hades.service.ReaderTextService;
import com.sun.hades.service.TextViewService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ReaderTextDataFetcherTest {

  @Mock private ReaderTextService textService;
  @Mock private ReaderSourceService sourceService;
  @Mock private InferenceClient inferenceClient;
  @Mock private ReaderTextMapper textMapper;
  @Mock private ReaderSourceMapper sourceMapper;
  @Mock private TextViewService textViewService;
  @Mock private WordReferenceService wordReferenceService;
  @Mock private DgsDataFetchingEnvironment env;

  @InjectMocks private ReaderTextDataFetcher fetcher;

  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUser() {
    UserContextHolder.setUserId(userId);
  }

  @AfterEach
  void clearUser() {
    UserContextHolder.clear();
  }

  @Test
  void getHadesQueries_shouldReturnNonNull() {
    assertThat(fetcher.getHadesQueries()).isNotNull();
  }

  @Test
  void getHadesMutations_shouldReturnNonNull() {
    assertThat(fetcher.getHadesMutations()).isNotNull();
  }

  @Test
  void texts_returnsPaged() {
    ReaderTextEntity entity = new ReaderTextEntity();
    entity.setId(UUID.randomUUID());
    entity.setTitle("Title");
    Page<ReaderTextEntity> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1);
    when(textService.list(any(), any())).thenReturn(page);
    ReaderText mapped = ReaderText.newBuilder().id(entity.getId().toString()).title("Title").build();
    when(textMapper.map(entity)).thenReturn(mapped);

    PagedReaderTexts result = fetcher.texts(null);

    assertThat(result.getItems()).hasSize(1);
    assertThat(result.getItems().get(0).getTitle()).isEqualTo("Title");
    assertThat(result.getPageInfo().getTotalCount()).isEqualTo(1);
  }

  @Test
  void text_returnsWhenFound() {
    UUID id = UUID.randomUUID();
    ReaderTextEntity entity = new ReaderTextEntity();
    entity.setId(id);
    entity.setTitle("Hello");
    when(textService.findById(id)).thenReturn(Optional.of(entity));
    ReaderText mapped = ReaderText.newBuilder().id(id.toString()).title("Hello").build();
    when(textMapper.map(entity)).thenReturn(mapped);

    ReaderText result = fetcher.text(id.toString());

    assertThat(result).isEqualTo(mapped);
    verify(textMapper).map(entity);
  }

  @Test
  void text_returnsNullWhenMissing() {
    UUID id = UUID.randomUUID();
    when(textService.findById(id)).thenReturn(Optional.empty());

    assertThat(fetcher.text(id.toString())).isNull();
  }

  @Test
  void textContent_returnsContent() {
    UUID id = UUID.randomUUID();
    ReaderText parent = ReaderText.newBuilder().id(id.toString()).title("T").build();
    ReaderTextEntity entity = new ReaderTextEntity();
    entity.setId(id);
    entity.setContent("content");
    when(env.getSource()).thenReturn(parent);
    when(textService.findById(id)).thenReturn(Optional.of(entity));

    String result = fetcher.textContent(env);

    assertThat(result).isEqualTo("content");
  }

  @Test
  void textContent_shouldReturnNullWhenNoParent() {
    when(env.getSource()).thenReturn(null);

    assertThat(fetcher.textContent(env)).isNull();
  }

  @Test
  void classifyTextLevel_delegates() {
    TextLevelAssessment assessment = TextLevelAssessment.newBuilder()
        .level(CefrLevel.B2).confidence(0.5f).build();
    when(inferenceClient.classify("some text")).thenReturn(Optional.of(assessment));

    assertThat(fetcher.classifyTextLevel("some text")).isSameAs(assessment);
  }

  @Test
  void classifyTextLevel_returnsNullWhenUnavailable() {
    when(inferenceClient.classify("some text")).thenReturn(Optional.empty());

    assertThat(fetcher.classifyTextLevel("some text")).isNull();
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
  void source_returns() {
    UUID id = UUID.randomUUID();
    ReaderSourceEntity entity = new ReaderSourceEntity();
    entity.setId(id);
    entity.setName("Wikipedia");
    when(sourceService.findById(id)).thenReturn(Optional.of(entity));
    ReaderSource mapped = ReaderSource.newBuilder().id(id.toString()).name("Wikipedia").build();
    when(sourceMapper.map(entity)).thenReturn(mapped);

    ReaderSource result = fetcher.source(id.toString());

    assertThat(result).isEqualTo(mapped);
  }

  @Test
  void source_returnsNullWhenMissing() {
    UUID id = UUID.randomUUID();
    when(sourceService.findById(id)).thenReturn(Optional.empty());

    assertThat(fetcher.source(id.toString())).isNull();
  }

  @Test
  void sources_returns() {
    ReaderSourceEntity entity = new ReaderSourceEntity();
    entity.setId(UUID.randomUUID());
    entity.setName("Src");
    when(sourceService.findAll()).thenReturn(List.of(entity));
    ReaderSource mapped = ReaderSource.newBuilder().id(entity.getId().toString()).name("Src").build();
    when(sourceMapper.map(entity)).thenReturn(mapped);

    List<ReaderSource> result = fetcher.sources();

    assertThat(result).containsExactly(mapped);
  }

  @Test
  void createSource_delegates() {
    ReaderSourceEntity saved = new ReaderSourceEntity();
    saved.setId(UUID.randomUUID());
    ReaderSource mapped = ReaderSource.newBuilder().id(saved.getId().toString()).name("Name").build();
    when(sourceService.save(any())).thenReturn(saved);
    when(sourceMapper.map(saved)).thenReturn(mapped);

    CreateSourceResponse result = fetcher.createSource("Name", "https://example.com");

    assertThat(result.getSource()).isEqualTo(mapped);
    assertThat(result.getMessage()).contains("created");
  }

  @Test
  void createSource_throwsWhenUnauthenticated() {
    UserContextHolder.clear();

    assertThatThrownBy(() -> fetcher.createSource("Name", "https://example.com"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void createText_delegates() {
    ReaderTextInput input = ReaderTextInput.newBuilder()
        .title("Title").content("content").language("fr").level(CefrLevel.A1).build();
    ReaderTextEntity entity = new ReaderTextEntity();
    when(textMapper.mapInput(input)).thenReturn(entity);
    ReaderTextEntity saved = new ReaderTextEntity();
    saved.setId(UUID.randomUUID());
    ReaderText mapped = ReaderText.newBuilder().id(saved.getId().toString()).title("Title").build();
    when(textService.save(entity)).thenReturn(saved);
    when(textMapper.map(saved)).thenReturn(mapped);

    CreateTextResponse result = fetcher.createText(input);

    assertThat(result.getText()).isEqualTo(mapped);
  }

  @Test
  void createText_throwsOnFailure() {
    ReaderTextInput input = ReaderTextInput.newBuilder()
        .title("Title").content("content").language("fr").level(CefrLevel.A1).build();
    when(textMapper.mapInput(input)).thenReturn(new ReaderTextEntity());
    when(textService.save(any())).thenThrow(new RuntimeException("fail"));

    assertThatThrownBy(() -> fetcher.createText(input))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("fail");
  }

  @Test
  void archiveText_delegates() {
    UUID id = UUID.randomUUID();
    ReaderTextEntity entity = new ReaderTextEntity();
    entity.setId(id);
    entity.setStatus(ReaderTextStatus.ACTIVE);
    ReaderTextEntity saved = new ReaderTextEntity();
    saved.setId(id);
    saved.setStatus(ReaderTextStatus.ARCHIVED);
    ReaderText mapped = ReaderText.newBuilder().id(id.toString()).title("Title").build();
    when(textService.findById(id)).thenReturn(Optional.of(entity));
    when(textService.save(any())).thenReturn(saved);
    when(textMapper.map(saved)).thenReturn(mapped);

    ArchiveTextResponse result = fetcher.archiveText(id.toString());

    assertThat(result.getText()).isEqualTo(mapped);
    assertThat(entity.getStatus()).isEqualTo(ReaderTextStatus.ARCHIVED);
  }

  @Test
  void archiveText_throwsWhenNotFound() {
    UUID id = UUID.randomUUID();
    when(textService.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> fetcher.archiveText(id.toString()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void markViewed_delegates() {
    UUID id = UUID.randomUUID();

    MarkViewedResponse result = fetcher.markViewed(id.toString());

    assertThat(result.getTextId()).isEqualTo(id.toString());
    verify(textViewService).markViewed(id);
  }

  @Test
  void markViewed_throwsOnFailure() {
    UUID id = UUID.randomUUID();
    org.mockito.Mockito.doThrow(new IllegalArgumentException("fail"))
        .when(textViewService).markViewed(id);

    assertThatThrownBy(() -> fetcher.markViewed(id.toString()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
