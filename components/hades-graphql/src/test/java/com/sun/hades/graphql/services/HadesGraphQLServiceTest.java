package com.sun.hades.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import com.sun.gaia.service.UserContextHolder;
import com.sun.hades.codegen.types.QueryResult;
import com.sun.hades.codegen.types.QuerySuccess;
import com.sun.hades.codegen.types.ReaderAccount;
import com.sun.hades.codegen.types.ReaderAnnotation;
import com.sun.hades.codegen.types.ReaderPosition;
import com.sun.hades.codegen.types.ReaderText;
import com.sun.hades.codegen.types.ReaderTextInput;
import com.sun.hades.codegen.types.StandardError;
import com.sun.hades.graphql.mappers.ReaderAccountMapper;
import com.sun.hades.graphql.mappers.ReaderAnnotationMapper;
import com.sun.hades.graphql.mappers.ReaderCommentMapper;
import com.sun.hades.graphql.mappers.ReaderSourceMapper;
import com.sun.hades.graphql.mappers.ReaderTextMapper;
import com.sun.hades.model.ReaderAccountEntity;
import com.sun.hades.model.ReaderAnnotationEntity;
import com.sun.hades.model.ReaderPositionEntity;
import com.sun.hades.model.ReaderTextEntity;
import com.sun.hades.model.enums.CefrLevel;
import com.sun.hades.model.enums.ReaderStatus;
import com.sun.hades.service.DiscordOAuthService;
import com.sun.hades.service.ReaderAccountService;
import com.sun.hades.service.ReaderAnnotationService;
import com.sun.hades.service.ReaderCommentService;
import com.sun.hades.service.ReaderPositionService;
import com.sun.hades.service.ReaderSourceService;
import com.sun.hades.service.ReaderTextService;
import com.sun.hades.service.ReaderVoteService;
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
class HadesGraphQLServiceTest {

  @Mock private ReaderTextService textService;
  @Mock private ReaderSourceService sourceService;
  @Mock private ReaderAnnotationService annotationService;
  @Mock private ReaderCommentService commentService;
  @Mock private ReaderVoteService voteService;
  @Mock private ReaderAccountService accountService;
  @Mock private ReaderPositionService positionService;
  @Mock private DiscordOAuthService discordOAuthService;
  @Mock private com.sun.gaia.service.AccountService gaiaAccountService;
  @Mock private com.sun.gaia.service.JwtService jwtService;
  @Mock private ReaderTextMapper textMapper;
  @Mock private ReaderSourceMapper sourceMapper;
  @Mock private ReaderAnnotationMapper annotationMapper;
  @Mock private ReaderCommentMapper commentMapper;
  @Mock private ReaderAccountMapper accountMapper;

  @InjectMocks private HadesGraphQLService service;

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
  void texts_shouldReturnMappedTexts() {
    ReaderTextEntity entity = new ReaderTextEntity();
    entity.setId(UUID.randomUUID());
    entity.setTitle("Title");
    Page<ReaderTextEntity> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1);
    when(textService.list(any(), any())).thenReturn(page);
    ReaderText mapped = ReaderText.newBuilder().id(entity.getId().toString()).title("Title").build();
    when(textMapper.map(entity)).thenReturn(mapped);

    List<ReaderText> result = service.texts(null).getItems();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTitle()).isEqualTo("Title");
  }

  @Test
  void annotations_shouldResolvePositionsAndMap() {
    UUID textId = UUID.randomUUID();
    UUID positionId = UUID.randomUUID();
    ReaderPositionEntity position = new ReaderPositionEntity();
    position.setId(positionId);
    position.setTextId(textId);
    position.setStartOffset(0);
    position.setEndOffset(10);
    ReaderAnnotationEntity annotation = new ReaderAnnotationEntity();
    annotation.setId(UUID.randomUUID());
    annotation.setPositionId(positionId);
    annotation.setBody("body");
    annotation.setStatus(ReaderStatus.ACTIVE);
    annotation.setCreatedBy(userId);
    when(positionService.listForText(textId)).thenReturn(List.of(position));
    when(annotationService.listForText(textId, false)).thenReturn(List.of(annotation));
    when(accountService.findByGaiaAccountId(userId)).thenReturn(Optional.empty());
    ReaderPosition mappedPosition = ReaderPosition.newBuilder()
        .id(positionId.toString()).textId(textId.toString()).startOffset(0).endOffset(10).build();
    ReaderAnnotation mapped = ReaderAnnotation.newBuilder()
        .id(annotation.getId().toString()).position(mappedPosition).body("body").build();
    when(annotationMapper.map(annotation, mappedPosition, null)).thenReturn(mapped);

    List<ReaderAnnotation> result = service.annotations(textId.toString(), false);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPosition().getStartOffset()).isZero();
  }

  @Test
  void annotations_shouldResolveAuthor() {
    UUID textId = UUID.randomUUID();
    UUID positionId = UUID.randomUUID();
    ReaderPositionEntity position = new ReaderPositionEntity();
    position.setId(positionId);
    position.setTextId(textId);
    position.setStartOffset(0);
    position.setEndOffset(10);
    ReaderAnnotationEntity annotation = new ReaderAnnotationEntity();
    annotation.setId(UUID.randomUUID());
    annotation.setPositionId(positionId);
    annotation.setBody("body");
    annotation.setStatus(ReaderStatus.ACTIVE);
    annotation.setCreatedBy(userId);
    ReaderAccountEntity accountEntity = new ReaderAccountEntity();
    accountEntity.setId(UUID.randomUUID());
    accountEntity.setGaiaAccountId(userId);
    accountEntity.setDiscordId("123");
    accountEntity.setDiscordUsername("user");
    ReaderAccount mappedAccount = ReaderAccount.newBuilder()
        .id(accountEntity.getId().toString())
        .discordId("123")
        .discordUsername("user")
        .build();
    when(positionService.listForText(textId)).thenReturn(List.of(position));
    when(annotationService.listForText(textId, false)).thenReturn(List.of(annotation));
    when(accountService.findByGaiaAccountId(userId)).thenReturn(Optional.of(accountEntity));
    when(accountMapper.map(accountEntity)).thenReturn(mappedAccount);
    ReaderPosition mappedPosition = ReaderPosition.newBuilder()
        .id(positionId.toString()).textId(textId.toString()).startOffset(0).endOffset(10).build();
    ReaderAnnotation mapped = ReaderAnnotation.newBuilder()
        .id(annotation.getId().toString()).position(mappedPosition).body("body")
        .author(mappedAccount).build();
    when(annotationMapper.map(annotation, mappedPosition, mappedAccount)).thenReturn(mapped);

    List<ReaderAnnotation> result = service.annotations(textId.toString(), false);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getAuthor()).isNotNull();
    assertThat(result.get(0).getAuthor().getDiscordUsername()).isEqualTo("user");
  }

  @Test
  void createText_shouldReturnQuerySuccess() {
    ReaderTextInput input = ReaderTextInput.newBuilder()
        .title("Title").content("content").language("fr").level(CefrLevel.A1).build();
    ReaderTextEntity entity = new ReaderTextEntity();
    when(textMapper.mapInput(input)).thenReturn(entity);
    ReaderTextEntity saved = new ReaderTextEntity();
    saved.setId(UUID.randomUUID());
    when(textService.save(entity)).thenReturn(saved);

    QueryResult result = service.createText(input);

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(saved.getId().toString());
  }

  @Test
  void createText_shouldReturnStandardErrorWhenServiceThrows() {
    ReaderTextInput input = ReaderTextInput.newBuilder()
        .title("Title").content("content").language("fr").level(CefrLevel.A1).build();
    when(textMapper.mapInput(input)).thenReturn(new ReaderTextEntity());
    when(textService.save(any())).thenThrow(new RuntimeException("Database error"));

    QueryResult result = service.createText(input);

    assertThat(result).isInstanceOf(StandardError.class);
    assertThat(((StandardError) result).getMessage()).contains("Database error");
  }

  @Test
  void createAnnotation_shouldReturnQuerySuccessWithId() {
    UUID id = UUID.randomUUID();
    when(annotationService.createAnnotation(any(), org.mockito.ArgumentMatchers.eq(0),
        org.mockito.ArgumentMatchers.eq(10), any())).thenReturn(id);

    QueryResult result = service.createAnnotation(UUID.randomUUID().toString(), 0, 10, "body");

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(id.toString());
  }
}
