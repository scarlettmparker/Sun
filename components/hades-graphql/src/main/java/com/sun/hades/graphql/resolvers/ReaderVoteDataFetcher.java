package com.sun.hades.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.hades.codegen.types.QueryResult;
import com.sun.hades.codegen.types.VoteInput;
import com.sun.hades.graphql.services.ReaderVoteGraphQLService;
import com.sun.hades.model.enums.ReaderVoteTarget;
import com.sun.hades.model.enums.VoteValue;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Data fetchers for the reader queries and mutations.
 */
@DgsComponent
public class ReaderVoteDataFetcher {

  private final ReaderVoteGraphQLService readerVoteGraphQLService;

  public ReaderVoteDataFetcher(ReaderVoteGraphQLService readerVoteGraphQLService) {
    this.readerVoteGraphQLService = readerVoteGraphQLService;
  }

  /**
   * Returns the caller's vote on a target.
   *
   * @param targetType the target type
   * @param targetId the target id
   * @return the vote value
   */
  @DgsData(parentType = "HadesQueries", field = "myVote")
  @PreAuthorize("@permissions.has('graphql.hades.myVote')")
  public VoteValue myVote(ReaderVoteTarget targetType, String targetId) {
    return readerVoteGraphQLService.myVote(targetType, targetId);
  }

  /**
   * Casts a vote.
   *
   * @param input the vote input
   * @return a QueryResult
   */
  @DgsData(parentType = "HadesMutations", field = "vote")
  @PreAuthorize("@permissions.has('graphql.hades.vote')")
  public QueryResult vote(VoteInput input) {
    return readerVoteGraphQLService.vote(input);
  }

  /**
   * Removes the caller's vote.
   *
   * @param targetType the target type
   * @param targetId the target id
   * @return a QueryResult
   */
  @DgsData(parentType = "HadesMutations", field = "removeVote")
  @PreAuthorize("@permissions.has('graphql.hades.removeVote')")
  public QueryResult removeVote(ReaderVoteTarget targetType, String targetId) {
    return readerVoteGraphQLService.removeVote(targetType, targetId);
  }
}
