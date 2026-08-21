package com.sun.hades.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.hades.codegen.types.QueryResult;
import com.sun.hades.codegen.types.QuerySuccess;
import com.sun.hades.codegen.types.VoteInput;
import com.sun.hades.graphql.services.ReaderVoteGraphQLService;
import com.sun.hades.model.enums.ReaderVoteTarget;
import com.sun.hades.model.enums.VoteValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReaderVoteDataFetcherTest {

  @Mock private ReaderVoteGraphQLService readerVoteGraphQLService;

  @InjectMocks private ReaderVoteDataFetcher fetcher;

  @Test
  void myVote_shouldDelegateToService() {
    when(readerVoteGraphQLService.myVote(ReaderVoteTarget.ANNOTATION, "target-id")).thenReturn(VoteValue.UP);

    VoteValue result = fetcher.myVote(ReaderVoteTarget.ANNOTATION, "target-id");

    assertThat(result).isEqualTo(VoteValue.UP);
    verify(readerVoteGraphQLService).myVote(ReaderVoteTarget.ANNOTATION, "target-id");
  }

  @Test
  void vote_shouldDelegateToService() {
    VoteInput input = VoteInput.newBuilder()
        .targetType(ReaderVoteTarget.ANNOTATION).targetId("id").value(VoteValue.UP).build();
    QueryResult mockResult = QuerySuccess.newBuilder().message("ok").id("id").build();
    when(readerVoteGraphQLService.vote(input)).thenReturn(mockResult);

    QueryResult result = fetcher.vote(input);

    assertThat(result).isEqualTo(mockResult);
    verify(readerVoteGraphQLService).vote(input);
  }

  @Test
  void removeVote_shouldDelegateToService() {
    QueryResult mockResult = QuerySuccess.newBuilder().message("ok").id("id").build();
    when(readerVoteGraphQLService.removeVote(ReaderVoteTarget.ANNOTATION, "id")).thenReturn(mockResult);

    QueryResult result = fetcher.removeVote(ReaderVoteTarget.ANNOTATION, "id");

    assertThat(result).isEqualTo(mockResult);
    verify(readerVoteGraphQLService).removeVote(ReaderVoteTarget.ANNOTATION, "id");
  }
}
