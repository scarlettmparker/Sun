package com.sun.hades.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.base.error.MutationException;
import com.sun.hades.codegen.types.ReaderAnnotation;
import com.sun.hades.codegen.types.ReaderComment;
import com.sun.hades.codegen.types.VoteInput;
import com.sun.hades.codegen.types.VoteResponse;
import com.sun.hades.graphql.mappers.ReaderAnnotationMapper;
import com.sun.hades.graphql.mappers.ReaderCommentMapper;
import com.sun.hades.model.ReaderAnnotationEntity;
import com.sun.hades.model.ReaderCommentEntity;
import com.sun.hades.model.enums.ReaderVoteTarget;
import com.sun.hades.model.enums.VoteValue;
import com.sun.hades.service.ReaderAnnotationService;
import com.sun.hades.service.ReaderCommentService;
import com.sun.hades.service.ReaderVoteService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReaderVoteGraphQLServiceTest {

  @Mock private ReaderVoteService voteService;
  @Mock private ReaderAnnotationService annotationService;
  @Mock private ReaderCommentService commentService;
  @Mock private ReaderAnnotationMapper annotationMapper;
  @Mock private ReaderCommentMapper commentMapper;

  @InjectMocks private ReaderVoteGraphQLService service;

  @Test
  void myVote_returnsWhenFound() {
    UUID targetId = UUID.randomUUID();
    when(voteService.myVote(ReaderVoteTarget.ANNOTATION, targetId)).thenReturn(Optional.of(VoteValue.UP));

    VoteValue result = service.myVote(ReaderVoteTarget.ANNOTATION, targetId.toString());

    assertThat(result).isEqualTo(VoteValue.UP);
  }

  @Test
  void myVote_returnsNullWhenMissing() {
    UUID targetId = UUID.randomUUID();
    when(voteService.myVote(ReaderVoteTarget.ANNOTATION, targetId)).thenReturn(Optional.empty());

    assertThat(service.myVote(ReaderVoteTarget.ANNOTATION, targetId.toString())).isNull();
  }

  @Test
  void vote_delegatesForAnnotation() {
    UUID targetId = UUID.randomUUID();
    VoteInput input = VoteInput.newBuilder()
        .targetType(ReaderVoteTarget.ANNOTATION).targetId(targetId.toString()).value(VoteValue.UP).build();
    ReaderAnnotationEntity entity = new ReaderAnnotationEntity();
    entity.setId(targetId);
    ReaderAnnotation mapped = ReaderAnnotation.newBuilder().id(targetId.toString()).body("body").build();
    when(voteService.vote(ReaderVoteTarget.ANNOTATION, targetId, VoteValue.UP)).thenReturn(targetId);
    when(annotationService.findById(targetId)).thenReturn(Optional.of(entity));
    when(annotationMapper.map(eq(entity), any(), any(), anyInt(), any())).thenReturn(mapped);

    VoteResponse result = service.vote(input);

    assertThat(result.getAnnotation()).isEqualTo(mapped);
    assertThat(result.getComment()).isNull();
  }

  @Test
  void vote_delegatesForComment() {
    UUID targetId = UUID.randomUUID();
    VoteInput input = VoteInput.newBuilder()
        .targetType(ReaderVoteTarget.COMMENT).targetId(targetId.toString()).value(VoteValue.DOWN).build();
    ReaderCommentEntity entity = new ReaderCommentEntity();
    entity.setId(targetId);
    ReaderComment mapped = ReaderComment.newBuilder().id(targetId.toString()).body("body").build();
    when(voteService.vote(ReaderVoteTarget.COMMENT, targetId, VoteValue.DOWN)).thenReturn(targetId);
    when(commentService.findById(targetId)).thenReturn(Optional.of(entity));
    when(commentMapper.map(eq(entity), any(), any())).thenReturn(mapped);

    VoteResponse result = service.vote(input);

    assertThat(result.getComment()).isEqualTo(mapped);
    assertThat(result.getAnnotation()).isNull();
  }

  @Test
  void vote_throwsOnFailure() {
    UUID targetId = UUID.randomUUID();
    VoteInput input = VoteInput.newBuilder()
        .targetType(ReaderVoteTarget.ANNOTATION).targetId(targetId.toString()).value(VoteValue.UP).build();
    when(voteService.vote(ReaderVoteTarget.ANNOTATION, targetId, VoteValue.UP))
        .thenThrow(new IllegalArgumentException("fail"));

    assertThatThrownBy(() -> service.vote(input))
        .isInstanceOf(MutationException.class);
  }

  @Test
  void removeVote_delegates() {
    UUID targetId = UUID.randomUUID();
    ReaderAnnotationEntity entity = new ReaderAnnotationEntity();
    entity.setId(targetId);
    ReaderAnnotation mapped = ReaderAnnotation.newBuilder().id(targetId.toString()).body("body").build();
    when(voteService.removeVote(ReaderVoteTarget.ANNOTATION, targetId)).thenReturn(targetId);
    when(annotationService.findById(targetId)).thenReturn(Optional.of(entity));
    when(annotationMapper.map(eq(entity), any(), any(), anyInt(), any())).thenReturn(mapped);

    VoteResponse result = service.removeVote(ReaderVoteTarget.ANNOTATION, targetId.toString());

    assertThat(result.getAnnotation()).isEqualTo(mapped);
    verify(voteService).removeVote(ReaderVoteTarget.ANNOTATION, targetId);
  }

  @Test
  void removeVote_throwsWhenNoVote() {
    UUID targetId = UUID.randomUUID();
    when(voteService.removeVote(ReaderVoteTarget.ANNOTATION, targetId))
        .thenThrow(new IllegalArgumentException("No vote to remove"));

    assertThatThrownBy(() -> service.removeVote(ReaderVoteTarget.ANNOTATION, targetId.toString()))
        .isInstanceOf(MutationException.class);
  }
}
