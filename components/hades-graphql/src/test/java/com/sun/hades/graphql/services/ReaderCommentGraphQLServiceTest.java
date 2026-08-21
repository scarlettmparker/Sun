package com.sun.hades.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.hades.codegen.types.CommentInput;
import com.sun.hades.codegen.types.PagedReaderComments;
import com.sun.hades.codegen.types.QuerySuccess;
import com.sun.hades.codegen.types.ReaderComment;
import com.sun.hades.codegen.types.StandardError;
import com.sun.hades.graphql.mappers.ReaderCommentMapper;
import com.sun.hades.graphql.mappers.RemoteUserMapper;
import com.sun.hades.model.ReaderCommentEntity;
import com.sun.hades.model.enums.ReaderStatus;
import com.sun.hades.model.enums.ReaderVoteTarget;
import com.sun.hades.service.ReaderAccountService;
import com.sun.hades.service.ReaderCommentService;
import com.sun.hades.service.ReaderVoteService;
import java.util.List;
import java.util.Map;
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
class ReaderCommentGraphQLServiceTest {

  @Mock private ReaderCommentService commentService;
  @Mock private ReaderAccountService accountService;
  @Mock private ReaderVoteService voteService;
  @Mock private ReaderCommentMapper commentMapper;
  @Mock private RemoteUserMapper remoteUserMapper;

  @InjectMocks private ReaderCommentGraphQLService service;

  @Test
  void comments_returnsPaged() {
    UUID annotationId = UUID.randomUUID();
    ReaderCommentEntity entity = new ReaderCommentEntity();
    entity.setId(UUID.randomUUID());
    entity.setAnnotationId(annotationId);
    entity.setBody("hello");
    entity.setStatus(ReaderStatus.ACTIVE);
    Page<ReaderCommentEntity> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 10), 1);
    when(commentService.listForAnnotation(eq(annotationId), any())).thenReturn(page);
    when(voteService.myVotes(eq(ReaderVoteTarget.COMMENT), anyList())).thenReturn(Map.of());
    ReaderComment mapped = ReaderComment.newBuilder().id(entity.getId().toString()).body("hello").build();
    when(commentMapper.map(eq(entity), any(), any())).thenReturn(mapped);

    PagedReaderComments result = service.comments(annotationId.toString(), false, null);

    assertThat(result.getItems()).hasSize(1);
    assertThat(result.getItems().get(0).getBody()).isEqualTo("hello");
    assertThat(result.getPageInfo().getTotalCount()).isEqualTo(1);
  }

  @Test
  void comments_filtersHiddenWhenNotIncluded() {
    UUID annotationId = UUID.randomUUID();
    ReaderCommentEntity active = new ReaderCommentEntity();
    active.setId(UUID.randomUUID());
    active.setAnnotationId(annotationId);
    active.setBody("active");
    active.setStatus(ReaderStatus.ACTIVE);
    ReaderCommentEntity hidden = new ReaderCommentEntity();
    hidden.setId(UUID.randomUUID());
    hidden.setAnnotationId(annotationId);
    hidden.setBody("hidden");
    hidden.setStatus(ReaderStatus.HIDDEN);
    Page<ReaderCommentEntity> page = new PageImpl<>(List.of(active, hidden), PageRequest.of(0, 10), 2);
    when(commentService.listForAnnotation(eq(annotationId), any())).thenReturn(page);
    when(voteService.myVotes(eq(ReaderVoteTarget.COMMENT), anyList())).thenReturn(Map.of());
    ReaderComment mapped = ReaderComment.newBuilder().id(active.getId().toString()).body("active").build();
    when(commentMapper.map(eq(active), any(), any())).thenReturn(mapped);

    PagedReaderComments result = service.comments(annotationId.toString(), false, null);

    assertThat(result.getItems()).hasSize(1);
    assertThat(result.getItems().get(0).getBody()).isEqualTo("active");
  }

  @Test
  void addComment_delegates() {
    UUID annotationId = UUID.randomUUID();
    UUID returned = UUID.randomUUID();
    CommentInput input = CommentInput.newBuilder()
        .annotationId(annotationId.toString()).body("body").build();
    when(commentService.addComment(eq(annotationId), eq(null), eq("body"))).thenReturn(returned);

    var result = service.addComment(input);

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(returned.toString());
  }

  @Test
  void addComment_withParentId_delegates() {
    UUID annotationId = UUID.randomUUID();
    UUID parentId = UUID.randomUUID();
    UUID returned = UUID.randomUUID();
    CommentInput input = CommentInput.newBuilder()
        .annotationId(annotationId.toString()).parentId(parentId.toString()).body("reply").build();
    when(commentService.addComment(eq(annotationId), eq(parentId), eq("reply"))).thenReturn(returned);

    var result = service.addComment(input);

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(returned.toString());
  }

  @Test
  void addComment_returnsStandardErrorOnFailure() {
    UUID annotationId = UUID.randomUUID();
    CommentInput input = CommentInput.newBuilder()
        .annotationId(annotationId.toString()).body("").build();
    when(commentService.addComment(any(), any(), any())).thenThrow(new IllegalArgumentException("Invalid comment"));

    var result = service.addComment(input);

    assertThat(result).isInstanceOf(StandardError.class);
  }

  @Test
  void editComment_delegates() {
    UUID id = UUID.randomUUID();
    UUID returned = UUID.randomUUID();
    when(commentService.editComment(id, "new")).thenReturn(returned);

    var result = service.editComment(id.toString(), "new");

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(returned.toString());
  }

  @Test
  void deleteComment_delegates() {
    UUID id = UUID.randomUUID();

    var result = service.deleteComment(id.toString());

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(id.toString());
    verify(commentService).deleteComment(id);
  }

  @Test
  void deleteComment_returnsStandardErrorWhenThrows() {
    UUID id = UUID.randomUUID();
    org.mockito.Mockito.doThrow(new IllegalArgumentException("not found"))
        .when(commentService).deleteComment(id);

    var result = service.deleteComment(id.toString());

    assertThat(result).isInstanceOf(StandardError.class);
  }
}
