package com.sun.hades.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.hades.codegen.types.QuerySuccess;
import com.sun.hades.codegen.types.StandardError;
import com.sun.hades.codegen.types.VoteInput;
import com.sun.hades.model.enums.ReaderVoteTarget;
import com.sun.hades.model.enums.VoteValue;
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
  void vote_delegates() {
    UUID targetId = UUID.randomUUID();
    VoteInput input = VoteInput.newBuilder()
        .targetType(ReaderVoteTarget.ANNOTATION).targetId(targetId.toString()).value(VoteValue.UP).build();
    when(voteService.vote(ReaderVoteTarget.ANNOTATION, targetId, VoteValue.UP)).thenReturn(targetId);

    var result = service.vote(input);

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(targetId.toString());
  }

  @Test
  void vote_returnsStandardErrorOnFailure() {
    UUID targetId = UUID.randomUUID();
    VoteInput input = VoteInput.newBuilder()
        .targetType(ReaderVoteTarget.ANNOTATION).targetId(targetId.toString()).value(VoteValue.UP).build();
    when(voteService.vote(ReaderVoteTarget.ANNOTATION, targetId, VoteValue.UP))
        .thenThrow(new IllegalArgumentException("fail"));

    var result = service.vote(input);

    assertThat(result).isInstanceOf(StandardError.class);
  }

  @Test
  void removeVote_delegates() {
    UUID targetId = UUID.randomUUID();
    when(voteService.removeVote(ReaderVoteTarget.ANNOTATION, targetId)).thenReturn(targetId);

    var result = service.removeVote(ReaderVoteTarget.ANNOTATION, targetId.toString());

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(targetId.toString());
    verify(voteService).removeVote(ReaderVoteTarget.ANNOTATION, targetId);
  }

  @Test
  void removeVote_returnsStandardErrorWhenNoVote() {
    UUID targetId = UUID.randomUUID();
    when(voteService.removeVote(ReaderVoteTarget.ANNOTATION, targetId))
        .thenThrow(new IllegalArgumentException("No vote to remove"));

    var result = service.removeVote(ReaderVoteTarget.ANNOTATION, targetId.toString());

    assertThat(result).isInstanceOf(StandardError.class);
  }
}
