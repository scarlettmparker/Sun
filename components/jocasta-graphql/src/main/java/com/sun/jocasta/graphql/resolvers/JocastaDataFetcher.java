package com.sun.jocasta.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.InputArgument;
import com.sun.jocasta.codegen.types.PagedAnswers;
import com.sun.jocasta.codegen.types.PagedQuestions;
import com.sun.jocasta.codegen.types.PaginationInput;
import com.sun.jocasta.codegen.types.Question;
import com.sun.jocasta.codegen.types.QuestionInput;
import com.sun.jocasta.codegen.types.QueryResult;
import com.sun.jocasta.graphql.services.JocastaGraphQLService;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@DgsComponent
public class JocastaDataFetcher {

  @Autowired
  private JocastaGraphQLService service;

  /**
   * Exposes questionQueries root.
   *
   * @return placeholder
   */
  @DgsData(parentType = "Query", field = "questionQueries")
  public Object questionQueries() {
    return new Object();
  }

  /**
   * Exposes questionMutations root.
   *
   * @return placeholder
   */
  @DgsData(parentType = "Mutation", field = "questionMutations")
  public Object questionMutations() {
    return new Object();
  }

  /**
   * Lists questions for a remote object.
   *
   * @param remoteObject - remote object filter
   * @param pagination - caller-provided pagination
   * @return paged questions
   */
  @DgsData(parentType = "QuestionQueries", field = "listQuestions")
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
  public QueryResult bulkCreateQuestions(@InputArgument List<QuestionInput> inputs) {
    return service.bulkCreateQuestions(inputs);
  }

  /**
   * Submits an answer attempt.
   *
   * @param questionId - question id
   * @param myAnswer - learner answer
   * @param correct - whether correct
   * @param correctAnswer - correct text
   * @return result
   */
  @DgsData(parentType = "QuestionMutations", field = "submitAnswer")
  public QueryResult submitAnswer(@InputArgument String questionId, @InputArgument String myAnswer, @InputArgument Boolean correct, @InputArgument String correctAnswer) {
    return service.submitAnswer(questionId, myAnswer, correct, correctAnswer);
  }

  /**
   * Links a question to an additional remote object.
   *
   * @param questionId - question id
   * @param target - remote object string
   * @return result
   */
  @DgsData(parentType = "QuestionMutations", field = "linkQuestion")
  public QueryResult linkQuestion(@InputArgument String questionId, @InputArgument String target) {
    return service.linkQuestion(questionId, target);
  }
}
