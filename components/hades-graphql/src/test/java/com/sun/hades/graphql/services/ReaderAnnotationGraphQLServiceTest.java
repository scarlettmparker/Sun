package com.sun.hades.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.hades.codegen.types.PagedReaderAnnotations;
import com.sun.hades.codegen.types.QuerySuccess;
import com.sun.hades.codegen.types.ReaderAnnotation;
import com.sun.hades.codegen.types.ReaderPosition;
import com.sun.hades.codegen.types.StandardError;
import com.sun.hades.graphql.mappers.ReaderAnnotationMapper;
import com.sun.hades.graphql.mappers.ReaderPositionMapper;
import com.sun.hades.graphql.mappers.RemoteUserMapper;
import com.sun.hades.model.ReaderAnnotationEntity;
import com.sun.hades.model.ReaderPositionEntity;
import com.sun.hades.model.enums.ReaderStatus;
import com.sun.hades.model.enums.ReaderVoteTarget;
import com.sun.hades.service.ReaderAccountService;
import com.sun.hades.service.ReaderAnnotationService;
import com.sun.hades.service.ReaderCommentService;
import com.sun.hades.service.ReaderPositionService;
import com.sun.hades.service.ReaderVoteService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ReaderAnnotationGraphQLServiceTest {

  @Mock private ReaderAnnotationService annotationService;
  @Mock private ReaderPositionService positionService;
  @Mock private ReaderAccountService accountService;
  @Mock private ReaderCommentService commentService;
  @Mock private ReaderVoteService voteService;
  @Mock private ReaderAnnotationMapper annotationMapper;
  @Mock private ReaderPositionMapper positionMapper;
  @Mock private RemoteUserMapper remoteUserMapper;

  @InjectMocks private ReaderAnnotationGraphQLService service;

  @Test
  void annotations_returnsPagedWithPositions() {
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
    Page<ReaderAnnotationEntity> page = new PageImpl<>(List.of(annotation), PageRequest.of(0, 10), 1);
    when(annotationService.listForTextPaged(eq(textId), eq(false), any())).thenReturn(page);
    when(positionService.listForText(textId)).thenReturn(List.of(position));
    when(voteService.myVotes(eq(ReaderVoteTarget.ANNOTATION), anyList())).thenReturn(Map.of());
    when(commentService.countByAnnotationIds(anyList())).thenReturn(Map.of());
    ReaderPosition mappedPosition = ReaderPosition.newBuilder()
        .id(positionId.toString()).textId(textId.toString()).startOffset(0).endOffset(10).build();
    ReaderAnnotation mapped = ReaderAnnotation.newBuilder()
        .id(annotation.getId().toString()).body("body").position(mappedPosition).build();
    when(positionMapper.map(position)).thenReturn(mappedPosition);
    when(annotationMapper.map(eq(annotation), eq(mappedPosition), any(), eq(0), any())).thenReturn(mapped);

    PagedReaderAnnotations result = service.annotations(textId.toString(), false, null);

    assertThat(result.getItems()).hasSize(1);
    assertThat(result.getItems().get(0).getBody()).isEqualTo("body");
    assertThat(result.getPageInfo().getTotalCount()).isEqualTo(1);
  }

  @Test
  void annotation_returnsWhenFound() {
    UUID id = UUID.randomUUID();
    ReaderAnnotationEntity entity = new ReaderAnnotationEntity();
    entity.setId(id);
    entity.setBody("body");
    entity.setStatus(ReaderStatus.ACTIVE);
    when(annotationService.findById(id)).thenReturn(Optional.of(entity));
    when(commentService.countByAnnotationIds(List.of(id))).thenReturn(Map.of(id, 2L));
    ReaderAnnotation mapped = ReaderAnnotation.newBuilder().id(id.toString()).body("body").replyCount(2).build();
    when(annotationMapper.map(eq(entity), any(), any(), eq(2), any())).thenReturn(mapped);

    ReaderAnnotation result = service.annotation(id.toString());

    assertThat(result).isEqualTo(mapped);
  }

  @Test
  void annotation_returnsNullWhenMissing() {
    UUID id = UUID.randomUUID();
    when(annotationService.findById(id)).thenReturn(Optional.empty());

    assertThat(service.annotation(id.toString())).isNull();
  }

  @Test
  void createAnnotation_success() {
    UUID id = UUID.randomUUID();
    UUID textId = UUID.randomUUID();
    when(annotationService.createAnnotation(eq(textId), eq(0), eq(10), eq("body"))).thenReturn(id);

    var result = service.createAnnotation(textId.toString(), 0, 10, "body");

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(id.toString());
  }

  @Test
  void createAnnotation_overlapErrorReturnsStandardError() {
    UUID textId = UUID.randomUUID();
    when(annotationService.createAnnotation(any(), any(int.class), any(int.class), any()))
        .thenThrow(new IllegalArgumentException("Range overlaps an active annotation"));

    var result = service.createAnnotation(textId.toString(), 0, 10, "body");

    assertThat(result).isInstanceOf(StandardError.class);
    assertThat(((StandardError) result).getMessage()).contains("overlaps");
  }

  @Test
  void editAnnotation_delegates() {
    UUID id = UUID.randomUUID();
    UUID returned = UUID.randomUUID();
    when(annotationService.editAnnotation(id, "new body")).thenReturn(returned);

    var result = service.editAnnotation(id.toString(), "new body");

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(returned.toString());
  }

  @Test
  void editAnnotation_returnsStandardErrorOnFailure() {
    UUID id = UUID.randomUUID();
    when(annotationService.editAnnotation(eq(id), any())).thenThrow(new IllegalArgumentException("Not the author"));

    var result = service.editAnnotation(id.toString(), "body");

    assertThat(result).isInstanceOf(StandardError.class);
    assertThat(((StandardError) result).getMessage()).contains("Not the author");
  }

  @Test
  void deleteAnnotation_delegates() {
    UUID id = UUID.randomUUID();

    var result = service.deleteAnnotation(id.toString());

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(id.toString());
    verify(annotationService).deleteAnnotation(id);
  }

  @Test
  void deleteAnnotation_returnsStandardErrorWhenServiceThrows() {
    UUID id = UUID.randomUUID();
    org.mockito.Mockito.doThrow(new IllegalArgumentException("not found"))
        .when(annotationService).deleteAnnotation(id);

    var result = service.deleteAnnotation(id.toString());

    assertThat(result).isInstanceOf(StandardError.class);
  }

  @Test
  void attachObject_delegates() {
    UUID source = UUID.randomUUID();
    UUID returned = UUID.randomUUID();
    when(annotationService.attach(eq(source), eq("target"))).thenReturn(returned);

    var result = service.attachObject(source.toString(), "target");

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(returned.toString());
  }
}
