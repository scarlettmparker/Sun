package com.sun.jocasta.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.InputArgument;
import com.sun.base.error.MutationException;
import com.sun.base.util.PageRequests;
import com.sun.jocasta.codegen.types.Answer;
import com.sun.jocasta.codegen.types.AnswerInput;
import com.sun.jocasta.codegen.types.BulkCreateQuestionsResponse;
import com.sun.jocasta.codegen.types.LinkQuestionResponse;
import com.sun.jocasta.codegen.types.PageInfo;
import com.sun.jocasta.codegen.types.PagedAnswers;
import com.sun.jocasta.codegen.types.PagedQuestions;
import com.sun.jocasta.codegen.types.PaginationInput;
import com.sun.jocasta.codegen.types.Question;
import com.sun.jocasta.codegen.types.QuestionInput;
import com.sun.jocasta.codegen.types.QuestionMutations;
import com.sun.jocasta.codegen.types.QuestionQueries;
import com.sun.jocasta.codegen.types.SubmitAnswerResponse;
import com.sun.jocasta.graphql.mappers.AnswerMapper;
import com.sun.jocasta.graphql.mappers.QuestionMapper;
import com.sun.jocasta.model.AnswerEntity;
import com.sun.jocasta.model.QuestionEntity;
import com.sun.jocasta.repository.QuestionRepository;
import com.sun.jocasta.service.JocastaAnswerService;
import com.sun.jocasta.service.JocastaQuestionService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

@DgsComponent
public class JocastaDataFetcher {

  private static final Logger logger = LoggerFactory.getLogger(JocastaDataFetcher.class);

  private final JocastaQuestionService questionService;
  private final JocastaAnswerService answerService;
  private final QuestionRepository questionRepository;
  private final QuestionMapper questionMapper;
  private final AnswerMapper answerMapper;

  public JocastaDataFetcher(JocastaQuestionService questionService,
      JocastaAnswerService answerService, QuestionRepository questionRepository,
      QuestionMapper questionMapper, AnswerMapper answerMapper) {
    this.questionService = questionService;
    this.answerService = answerService;
    this.questionRepository = questionRepository;
    this.questionMapper = questionMapper;
    this.answerMapper = answerMapper;
  }

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
  @Transactional(readOnly = true)
  public PagedQuestions listQuestions(@InputArgument String remoteObject, @InputArgument PaginationInput pagination) {
    Pageable pageable = toPageable(pagination);
    Page<QuestionEntity> page;
    if (remoteObject != null && !remoteObject.isBlank()) {
      page = questionService.listByRemoteObject(remoteObject, pageable);
    } else {
      page = questionRepository.findAll(pageable);
    }
    List<Question> items = page.getContent().stream().map(questionMapper::map).toList();
    return PagedQuestions.newBuilder().items(items).pageInfo(pageInfo(page)).build();
  }

  /**
   * Locates a single question.
   *
   * @param id - question id
   * @return question
   */
  @DgsData(parentType = "QuestionQueries", field = "locateQuestion")
  @PreAuthorize("@permissions.has('graphql.jocasta.locateQuestion')")
  @Transactional(readOnly = true)
  public Question locateQuestion(@InputArgument String id) {
    QuestionEntity e = questionService.findById(UUID.fromString(id))
        .orElseThrow(() -> new RuntimeException("Question not found: " + id));
    return questionMapper.map(e);
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
  @Transactional(readOnly = true)
  public PagedAnswers listAnswers(@InputArgument String questionId, @InputArgument PaginationInput pagination) {
    Pageable pageable = toPageable(pagination);
    Page<AnswerEntity> page = answerService.listByQuestion(UUID.fromString(questionId), pageable);
    List<Answer> items = page.getContent().stream().map(answerMapper::map).toList();
    return PagedAnswers.newBuilder().items(items).pageInfo(pageInfo(page)).build();
  }

  /**
   * Bulk creates questions.
   *
   * @param inputs - question inputs
   * @return the created questions
   */
  @DgsData(parentType = "QuestionMutations", field = "bulkCreateQuestions")
  @PreAuthorize("@permissions.has('graphql.jocasta.bulkCreateQuestions')")
  @Transactional
  public BulkCreateQuestionsResponse bulkCreateQuestions(@InputArgument List<QuestionInput> inputs) {
    try {
      if (inputs == null || inputs.isEmpty()) {
        throw new IllegalArgumentException("Inputs required");
      }
      List<QuestionEntity> entities = new ArrayList<>();
      for (QuestionInput input : inputs) {
        if (input.getStem() == null || input.getStem().isBlank()) {
          throw new IllegalArgumentException("Stem required");
        }
        if (input.getAnswer() == null || input.getAnswer().isBlank()) {
          throw new IllegalArgumentException("Answer required");
        }
        entities.add(questionMapper.mapInput(input));
      }
      List<QuestionEntity> saved = questionService.createBulk(entities);
      List<Question> questions = saved.stream().map(questionMapper::map).toList();
      logger.info("bulkCreateQuestions succeeded for {} questions", questions.size());
      return BulkCreateQuestionsResponse.newBuilder()
          .message("bulkCreateQuestions succeeded")
          .questions(questions)
          .build();
    } catch (Exception e) {
      logger.error("bulkCreateQuestions failed", e);
      throw new MutationException("bulkCreateQuestions failed: " + e.getMessage(), e);
    }
  }

  /**
   * Submits an answer attempt for a question located by questionId.
   *
   * @param questionId - question id
   * @param input - answer input containing myAnswer, correct, correctAnswer
   * @return the submitted answer
   */
  @DgsData(parentType = "QuestionMutations", field = "submitAnswer")
  @PreAuthorize("@permissions.has('graphql.jocasta.submitAnswer')")
  @Transactional
  public SubmitAnswerResponse submitAnswer(@InputArgument String questionId, @InputArgument("input") AnswerInput input) {
    try {
      UUID qid = UUID.fromString(questionId);
      questionService.findById(qid)
          .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));
      AnswerEntity e = answerMapper.mapInput(questionId, input);
      AnswerEntity saved = answerService.submit(e);
      logger.info("submitAnswer succeeded for id {}", saved.getId());
      return SubmitAnswerResponse.newBuilder()
          .message("submitAnswer succeeded")
          .answer(answerMapper.map(saved))
          .build();
    } catch (Exception e) {
      logger.error("submitAnswer failed", e);
      throw new MutationException("submitAnswer failed: " + e.getMessage(), e);
    }
  }

  /**
   * Links a question to an additional remote object.
   *
   * @param questionId - question id
   * @param target - remote object string
   * @return the question id
   */
  @DgsData(parentType = "QuestionMutations", field = "linkQuestion")
  @PreAuthorize("@permissions.has('graphql.jocasta.linkQuestion')")
  @Transactional
  public LinkQuestionResponse linkQuestion(@InputArgument String questionId, @InputArgument String target) {
    try {
      UUID qid = UUID.fromString(questionId);
      if (target == null || target.isBlank()) {
        throw new IllegalArgumentException("Target required");
      }
      questionService.linkQuestion(qid, target)
          .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));
      logger.info("linkQuestion succeeded for id {}", qid);
      return LinkQuestionResponse.newBuilder()
          .message("linkQuestion succeeded")
          .id(qid.toString())
          .build();
    } catch (Exception e) {
      logger.error("linkQuestion failed", e);
      throw new MutationException("linkQuestion failed: " + e.getMessage(), e);
    }
  }

  /**
   * Converts pagination input to pageable without hard-coded defaults.
   *
   * @param pagination - pagination from caller
   * @return pageable
   */
  private Pageable toPageable(PaginationInput pagination) {
    if (pagination == null) {
      return PageRequests.of(null, null, null, null, null, null);
    }
    if (pagination.getSorts() != null && !pagination.getSorts().isEmpty()) {
      List<Sort.Order> orders = pagination.getSorts().stream()
          .map(s -> new Sort.Order(Sort.Direction.valueOf(s.getDir().name()), s.getField()))
          .toList();
      return PageRequests.of(pagination.getPage(), pagination.getSize(), orders, null, null);
    }
    return PageRequests.of(pagination.getPage(), pagination.getSize(), pagination.getSortBy(),
        pagination.getSortDir() == null ? null : pagination.getSortDir().name(), null, null);
  }

  /**
   * Builds page info from Spring page.
   *
   * @param result - data page
   * @return page info
   */
  private PageInfo pageInfo(Page<?> result) {
    return PageInfo.newBuilder()
        .page(result.getNumber())
        .size(result.getSize())
        .totalPages(result.getTotalPages())
        .totalCount((int) result.getTotalElements())
        .hasNextPage(result.hasNext())
        .hasPreviousPage(result.hasPrevious())
        .build();
  }
}
