package com.sun.jocasta.graphql.services;

import com.sun.base.error.MutationException;
import com.sun.base.util.PageRequests;
import com.sun.jocasta.codegen.types.Answer;
import com.sun.jocasta.codegen.types.BulkCreateQuestionsResponse;
import com.sun.jocasta.codegen.types.LinkQuestionResponse;
import com.sun.jocasta.codegen.types.PagedAnswers;
import com.sun.jocasta.codegen.types.PagedQuestions;
import com.sun.jocasta.codegen.types.PageInfo;
import com.sun.jocasta.codegen.types.PaginationInput;
import com.sun.jocasta.codegen.types.SubmitAnswerResponse;
import com.sun.jocasta.codegen.types.AnswerInput;
import com.sun.jocasta.codegen.types.Question;
import com.sun.jocasta.codegen.types.QuestionInput;
import com.sun.jocasta.graphql.mappers.AnswerMapper;
import com.sun.jocasta.graphql.mappers.QuestionMapper;
import com.sun.jocasta.model.AnswerEntity;
import com.sun.jocasta.model.QuestionEntity;
import com.sun.jocasta.repository.QuestionRepository;
import com.sun.jocasta.service.JocastaAnswerService;
import com.sun.jocasta.service.JocastaQuestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class JocastaGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(JocastaGraphQLService.class);

  @Autowired
  private JocastaQuestionService questionService;

  @Autowired
  private JocastaAnswerService answerService;

  @Autowired
  private QuestionRepository questionRepository;

  @Autowired
  private QuestionMapper questionMapper;

  @Autowired
  private AnswerMapper answerMapper;

  /**
   * Lists questions, optionally filtered by remote object.
   *
   * @param remoteObject - remote object filter
   * @param pagination - callers pageable, no hard-coded defaults
   * @return paged questions
   */
  @Transactional(readOnly = true)
  public PagedQuestions listQuestions(String remoteObject, PaginationInput pagination) {
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
  @Transactional(readOnly = true)
  public Question locateQuestion(String id) {
    QuestionEntity e = questionService.findById(UUID.fromString(id))
        .orElseThrow(() -> new RuntimeException("Question not found: " + id));
    return questionMapper.map(e);
  }

  /**
   * Lists answers for a question.
   *
   * @param questionId - question id
   * @param pagination - callers pageable, no hard-coded defaults
   * @return paged answers
   */
  @Transactional(readOnly = true)
  public PagedAnswers listAnswers(String questionId, PaginationInput pagination) {
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
  @Transactional
  public BulkCreateQuestionsResponse bulkCreateQuestions(List<QuestionInput> inputs) {
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
   * Submits an answer attempt for a question.
   *
   * @param questionId - question id
   * @param input - answer input containing myAnswer, correct, correctAnswer
   * @return the submitted answer
   */
  @Transactional
  public SubmitAnswerResponse submitAnswer(String questionId, AnswerInput input) {
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
   * Links an existing question to an additional remote object.
   *
   * @param questionId - question id
   * @param target - remote object target
   * @return the question id
   */
  @Transactional
  public LinkQuestionResponse linkQuestion(String questionId, String target) {
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
