package com.sun.hades.graphql.services;

import com.sun.hades.codegen.types.QueryResult;
import com.sun.hades.codegen.types.QuerySuccess;
import com.sun.hades.codegen.types.StandardError;
import com.sun.hades.codegen.types.VoteInput;
import com.sun.hades.model.enums.ReaderVoteTarget;
import com.sun.hades.model.enums.VoteValue;
import com.sun.hades.service.ReaderVoteService;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL business logic for the reader.
 */
@Service
public class ReaderVoteGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(ReaderVoteGraphQLService.class);

  private final ReaderVoteService voteService;

  public ReaderVoteGraphQLService(ReaderVoteService voteService) {
    this.voteService = voteService;
  }

  /**
   * Returns the caller's vote on a target.
   *
   * @param targetType the target type
   * @param targetId the target id
   * @return the vote value, or null
   */
  @Transactional(readOnly = true)
  public VoteValue myVote(ReaderVoteTarget targetType, String targetId) {
    return voteService.myVote(targetType, UUID.fromString(targetId)).orElse(null);
  }

  /**
   * Casts, toggles, or flips a vote.
   *
   * @param input the vote input
   * @return a QueryResult
   */
  @Transactional
  public QueryResult vote(VoteInput input) {
    return mutate("vote", () -> voteService.vote(
        input.getTargetType(),
        UUID.fromString(input.getTargetId()),
        input.getValue()));
  }

  /**
   * Removes the caller's vote.
   *
   * @param targetType the target type
   * @param targetId the target id
   * @return a QueryResult
   */
  @Transactional
  public QueryResult removeVote(ReaderVoteTarget targetType, String targetId) {
    return mutate("removeVote",
        () -> voteService.removeVote(targetType, UUID.fromString(targetId)));
  }

  /**
   * Runs a mutation, returning QuerySuccess with the affected id or StandardError
   * on failure.
   *
   * @param op the operation name (for logging and messages)
   * @param action the mutation, returning the affected entity id
   * @return a QueryResult
   */
  private QueryResult mutate(String op, Supplier<UUID> action) {
    try {
      UUID id = action.get();
      logger.info("{} succeeded for id {}", op, id);
      return QuerySuccess.newBuilder()
          .message(op + " succeeded")
          .id(id == null ? null : id.toString())
          .build();
    } catch (Exception e) {
      logger.error("{} failed", op, e);
      return StandardError.newBuilder()
          .message(op + " failed: " + e.getMessage())
          .build();
    }
  }
}
