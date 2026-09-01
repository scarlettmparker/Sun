package com.sun.jocasta.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.InputArgument;
import com.sun.jocasta.codegen.types.PagedAnswers;
import com.sun.jocasta.codegen.types.PagedQuestions;
import com.sun.jocasta.codegen.types.PaginationInput;
import com.sun.jocasta.codegen.types.AnswerInput;
import com.sun.jocasta.codegen.types.Question;
import com.sun.jocasta.codegen.types.QuestionInput;
import com.sun.jocasta.codegen.types.QuestionMutations;
import com.sun.jocasta.codegen.types.QuestionQueries;
import com.sun.jocasta.codegen.types.QueryResult;
import com.sun.jocasta.graphql.services.JocastaGraphQLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;

@DgsComponent
public class JocastaDataFetcher {

  @Autowired
  private JocastaGraphQLService service;

  /**
   * Exposes questionQueries root.
   *
   * @return question queries placeholder
   */
  @DgsData(parentType = "Query", field = "questionQueries")
  public QuestionQueries questionQueries() {
    return QuestionQueries.newBuilder().build();
  }

  /**
   * Exposes questionMutations root.
   *
   * @return question mutations placeholder
   */
  @DgsData(parentType = "Mutation", field = "questionMutations")
  public QuestionMutations questionMutations() {
    return QuestionMutations.newBuilder().build();
  }

  /**
   * Lists questions for a remote object.
   *
   * @param remoteObject - remote object filter
   * @param pagination - caller-provided pagination
   * @return paged questions
   */
  @DgsData(parentType = "QuestionQueries", field = "listQuestions")
  @PreAuthorize("@permissions.has('graphql.jocasta.listQuestions')")
  public PagedQuestions listQuestions(@InputArgument String remoteObject, @InputArgument PaginationInput pagination) {
    return service.listQuestions(remoteObject, pagination);
  }

  /**
   * Locates a single question.
   *
   * @param id - question id
   * @return question
   */
  @DgsData(parentType = "QuestionQueries", field = "locateQuestion")
  @PreAuthorize("@permissions.has('graphql.jocasta.locateQuestion')")
  public Question locateQuestion(@InputArgument String id) {
    return service.locateQuestion(id);
  }

  /**
   * Lists answers for a question.
   *
   * @param questionId - question id
   * @param pagination - caller-provided pagination
   * @return paged answers
   */
  @DgsData(parentType = "QuestionQueries", field = "listAnswers")
  @PreAuthorize("@permissions.has('graphql.jocasta.listAnswers')")
  public PagedAnswers listAnswers(@InputArgument String questionId, @InputArgument PaginationInput pagination) {
    return service.listAnswers(questionId, pagination);
  }

  /**
   * Bulk creates questions.
   *
   * @param inputs - question inputs
   * @return result
   */
  @DgsData(parentType = "QuestionMutations", field = "bulkCreateQuestions")
  @PreAuthorize("@permissions.has('graphql.jocasta.bulkCreateQuestions')")
  public QueryResult bulkCreateQuestions(@InputArgument List<QuestionInput> inputs) {
    return service.bulkCreateQuestions(inputs);
  }

  /**
   * Submits an answer attempt for a question located by questionId.
   *
   * @param questionId - question id
   * @param input - answer input containing myAnswer, correct, correctAnswer
   * @return result
   */
  @DgsData(parentType = "QuestionMutations", field = "submitAnswer")
  @PreAuthorize("@permissions.has('graphql.jocasta.submitAnswer')")
  public QueryResult submitAnswer(@InputArgument String questionId, @InputArgument("input") AnswerInput input) {
    return service.submitAnswer(questionId, input);
  }

  /**
   * Links a question to an additional remote object.
   *
   * @param questionId - question id
   * @param target - remote object string
   * @return result
   */
  @DgsData(parentType = "QuestionMutations", field = "linkQuestion")
  @PreAuthorize("@permissions.has('graphql.jocasta.linkQuestion')")
  public QueryResult linkQuestion(@InputArgument String questionId, @InputArgument String target) {
    return service.linkQuestion(questionId, target);
  }
}
