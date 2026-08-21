package com.sun.hades.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.service.UserContextHolder;
import com.sun.hades.codegen.types.PagedReaderTexts;
import com.sun.hades.codegen.types.QuerySuccess;
import com.sun.hades.codegen.types.ReaderSource;
import com.sun.hades.codegen.types.ReaderText;
import com.sun.hades.codegen.types.ReaderTextInput;
import com.sun.hades.codegen.types.StandardError;
import com.sun.hades.codegen.types.TextLevelAssessment;
import com.sun.hades.graphql.inference.InferenceClient;
import com.sun.hades.graphql.mappers.ReaderSourceMapper;
import com.sun.hades.graphql.mappers.ReaderTextMapper;
import com.sun.hades.model.ReaderSourceEntity;
import com.sun.hades.model.ReaderTextEntity;
import com.sun.hades.model.enums.CefrLevel;
import com.sun.hades.model.enums.ReaderTextStatus;
import com.sun.hades.service.ReaderSourceService;
import com.sun.hades.service.ReaderTextService;
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
class ReaderTextGraphQLServiceTest {

  @Mock private ReaderTextService textService;
  @Mock private ReaderSourceService sourceService;
  @Mock private InferenceClient inferenceClient;
  @Mock private ReaderTextMapper textMapper;
  @Mock private ReaderSourceMapper sourceMapper;

  @InjectMocks private ReaderTextGraphQLService service;

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
  void texts_returnsPaged() {
    ReaderTextEntity entity = new ReaderTextEntity();
    entity.setId(UUID.randomUUID());
    entity.setTitle("Title");
    Page<ReaderTextEntity> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1);
    when(textService.list(any(), any())).thenReturn(page);
    ReaderText mapped = ReaderText.newBuilder().id(entity.getId().toString()).title("Title").build();
    when(textMapper.map(entity)).thenReturn(mapped);

    PagedReaderTexts result = service.texts(null);

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

    ReaderText result = service.text(id.toString());

    assertThat(result).isEqualTo(mapped);
    verify(textMapper).map(entity);
  }

  @Test
  void text_returnsNullWhenMissing() {
    UUID id = UUID.randomUUID();
    when(textService.findById(id)).thenReturn(Optional.empty());

    assertThat(service.text(id.toString())).isNull();
  }

  @Test
  void classifyTextLevel_delegates() {
    TextLevelAssessment assessment = TextLevelAssessment.newBuilder()
        .level(CefrLevel.B2).confidence(0.5f).build();
    when(inferenceClient.classify("some text")).thenReturn(Optional.of(assessment));

    assertThat(service.classifyTextLevel("some text")).isSameAs(assessment);
  }

  @Test
  void classifyTextLevel_returnsNullWhenUnavailable() {
    when(inferenceClient.classify("some text")).thenReturn(Optional.empty());

    assertThat(service.classifyTextLevel("some text")).isNull();
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

    ReaderSource result = service.source(id.toString());

    assertThat(result).isEqualTo(mapped);
  }

  @Test
  void source_returnsNullWhenMissing() {
    UUID id = UUID.randomUUID();
    when(sourceService.findById(id)).thenReturn(Optional.empty());

    assertThat(service.source(id.toString())).isNull();
  }

  @Test
  void sources_returns() {
    ReaderSourceEntity entity = new ReaderSourceEntity();
    entity.setId(UUID.randomUUID());
    entity.setName("Src");
    when(sourceService.findAll()).thenReturn(List.of(entity));
    ReaderSource mapped = ReaderSource.newBuilder().id(entity.getId().toString()).name("Src").build();
    when(sourceMapper.map(entity)).thenReturn(mapped);

    List<ReaderSource> result = service.sources();

    assertThat(result).containsExactly(mapped);
  }

  @Test
  void createSource_delegates() {
    ReaderSourceEntity saved = new ReaderSourceEntity();
    saved.setId(UUID.randomUUID());
    when(sourceService.save(any())).thenReturn(saved);

    var result = service.createSource("Name", "https://example.com");

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(saved.getId().toString());
  }

  @Test
  void createSource_returnsStandardErrorWhenUnauthenticated() {
    UserContextHolder.clear();

    var result = service.createSource("Name", "https://example.com");

    assertThat(result).isInstanceOf(StandardError.class);
  }

  @Test
  void createText_delegates() {
    ReaderTextInput input = ReaderTextInput.newBuilder()
        .title("Title").content("content").language("fr").level(CefrLevel.A1).build();
    ReaderTextEntity entity = new ReaderTextEntity();
    when(textMapper.mapInput(input)).thenReturn(entity);
    ReaderTextEntity saved = new ReaderTextEntity();
    saved.setId(UUID.randomUUID());
    when(textService.save(entity)).thenReturn(saved);

    var result = service.createText(input);

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(saved.getId().toString());
  }

  @Test
  void createText_returnsStandardErrorOnFailure() {
    ReaderTextInput input = ReaderTextInput.newBuilder()
        .title("Title").content("content").language("fr").level(CefrLevel.A1).build();
    when(textMapper.mapInput(input)).thenReturn(new ReaderTextEntity());
    when(textService.save(any())).thenThrow(new RuntimeException("fail"));

    var result = service.createText(input);

    assertThat(result).isInstanceOf(StandardError.class);
    assertThat(((StandardError) result).getMessage()).contains("fail");
  }

  @Test
  void archiveText_delegates() {
    UUID id = UUID.randomUUID();
    ReaderTextEntity entity = new ReaderTextEntity();
    entity.setId(id);
    entity.setStatus(ReaderTextStatus.ACTIVE);
    when(textService.findById(id)).thenReturn(Optional.of(entity));
    ReaderTextEntity saved = new ReaderTextEntity();
    saved.setId(id);
    when(textService.save(any())).thenReturn(saved);

    var result = service.archiveText(id.toString());

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(id.toString());
    assertThat(entity.getStatus()).isEqualTo(ReaderTextStatus.ARCHIVED);
  }

  @Test
  void archiveText_returnsStandardErrorWhenNotFound() {
    UUID id = UUID.randomUUID();
    when(textService.findById(id)).thenReturn(Optional.empty());

    var result = service.archiveText(id.toString());

    assertThat(result).isInstanceOf(StandardError.class);
  }
}
