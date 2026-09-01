package com.sun.jocasta.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.jocasta.codegen.types.Answer;
import com.sun.jocasta.codegen.types.AnswerInput;
import com.sun.jocasta.codegen.types.PagedAnswers;
import com.sun.jocasta.codegen.types.PagedQuestions;
import com.sun.jocasta.codegen.types.Question;
import com.sun.jocasta.codegen.types.QuestionInput;
import com.sun.jocasta.codegen.types.QuerySuccess;
import com.sun.jocasta.codegen.types.StandardError;
import com.sun.jocasta.graphql.mappers.AnswerMapper;
import com.sun.jocasta.graphql.mappers.QuestionMapper;
import com.sun.jocasta.model.AnswerEntity;
import com.sun.jocasta.model.QuestionEntity;
import com.sun.jocasta.repository.QuestionRepository;
import com.sun.jocasta.service.JocastaAnswerService;
import com.sun.jocasta.service.JocastaQuestionService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@DisplayName("JocastaGraphQLService")
@ExtendWith(MockitoExtension.class)
class JocastaGraphQLServiceTest {

  @Mock private JocastaQuestionService questionService;
  @Mock private JocastaAnswerService answerService;
  @Mock private QuestionRepository questionRepository;
  @Mock private QuestionMapper questionMapper;
  @Mock private AnswerMapper answerMapper;

  @InjectMocks private JocastaGraphQLService service;

  @Test
  @DisplayName("listQuestions with remoteObject returns paged")
  void listQuestions_withRemoteObject_returnsPaged() {
    QuestionEntity entity = new QuestionEntity();
    entity.setId(UUID.randomUUID());
    entity.setStem("What is ____ [concept]?");
    Page<QuestionEntity> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1);
    when(questionService.listByRemoteObject("briareus:post:123", any())).thenReturn(page);
    Question mapped = Question.newBuilder().id(entity.getId().toString()).stem("What is ____ [concept]?").answer("a").build();
    when(questionMapper.map(entity)).thenReturn(mapped);

    PagedQuestions result = service.listQuestions("briareus:post:123", null);

    assertThat(result.getItems()).hasSize(1);
    assertThat(result.getPageInfo().getTotalCount()).isEqualTo(1);
    verify(questionService).listByRemoteObject("briareus:post:123", any());
  }

  @Test
  @DisplayName("listQuestions without remoteObject returns all")
  void listQuestions_withoutRemoteObject_returnsAll() {
    QuestionEntity entity = new QuestionEntity();
    entity.setId(UUID.randomUUID());
    Page<QuestionEntity> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1);
    when(questionRepository.findAll(any(PageRequest.class))).thenReturn(page);
    Question mapped = Question.newBuilder().id(entity.getId().toString()).stem("s").answer("a").build();
    when(questionMapper.map(entity)).thenReturn(mapped);

    PagedQuestions result = service.listQuestions(null, null);

    assertThat(result.getItems()).hasSize(1);
  }

  @Test
  @DisplayName("locateQuestion returns when found")
  void locateQuestion_returnsWhenFound() {
    UUID id = UUID.randomUUID();
    QuestionEntity entity = new QuestionEntity();
    entity.setId(id);
    when(questionService.findById(id)).thenReturn(Optional.of(entity));
    Question mapped = Question.newBuilder().id(id.toString()).stem("s").answer("a").build();
    when(questionMapper.map(entity)).thenReturn(mapped);

    Question result = service.locateQuestion(id.toString());

    assertThat(result).isEqualTo(mapped);
  }

  @Test
  @DisplayName("bulkCreateQuestions delegates via mapper")
  void bulkCreateQuestions_delegatesViaMapper() {
    QuestionInput input = QuestionInput.newBuilder().stem("What is ____ [concept]?").answer("answer").build();
    QuestionEntity entity = new QuestionEntity();
    entity.setStem("What is ____ [concept]?");
    QuestionEntity saved = new QuestionEntity();
    saved.setId(UUID.randomUUID());
    when(questionMapper.mapInput(input)).thenReturn(entity);
    when(questionService.createBulk(any())).thenReturn(List.of(saved));

    var result = service.bulkCreateQuestions(List.of(input));

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(saved.getId().toString());
    verify(questionMapper).mapInput(input);
    verify(questionService).createBulk(any());
  }

  @Test
  @DisplayName("bulkCreateQuestions returns error when empty")
  void bulkCreateQuestions_returnsErrorWhenEmpty() {
    var result = service.bulkCreateQuestions(List.of());

    assertThat(result).isInstanceOf(StandardError.class);
  }

  @Test
  @DisplayName("submitAnswer delegates via mapper")
  void submitAnswer_delegatesViaMapper() {
    UUID qid = UUID.randomUUID();
    QuestionEntity q = new QuestionEntity();
    q.setId(qid);
    when(questionService.findById(qid)).thenReturn(Optional.of(q));
    AnswerInput input = AnswerInput.newBuilder().myAnswer("my").correct(true).correctAnswer("corr").build();
    AnswerEntity mapped = new AnswerEntity();
    mapped.setQuestionId(qid);
    when(answerMapper.mapInput(qid.toString(), input)).thenReturn(mapped);
    AnswerEntity saved = new AnswerEntity();
    saved.setId(UUID.randomUUID());
    when(answerService.submit(mapped)).thenReturn(saved);

    var result = service.submitAnswer(qid.toString(), input);

    assertThat(result).isInstanceOf(QuerySuccess.class);
    verify(answerMapper).mapInput(qid.toString(), input);
    verify(answerService).submit(mapped);
  }

  @Test
  @DisplayName("submitAnswer returns error when question missing")
  void submitAnswer_returnsErrorWhenQuestionMissing() {
    UUID qid = UUID.randomUUID();
    when(questionService.findById(qid)).thenReturn(Optional.empty());
    AnswerInput input = AnswerInput.newBuilder().myAnswer("my").correct(true).correctAnswer("corr").build();

    var result = service.submitAnswer(qid.toString(), input);

    assertThat(result).isInstanceOf(StandardError.class);
  }

  @Test
  @DisplayName("linkQuestion delegates")
  void linkQuestion_delegates() {
    UUID qid = UUID.randomUUID();
    QuestionEntity entity = new QuestionEntity();
    entity.setId(qid);
    when(questionService.linkQuestion(qid, "briareus:post:999")).thenReturn(Optional.of(entity));

    var result = service.linkQuestion(qid.toString(), "briareus:post:999");

    assertThat(result).isInstanceOf(QuerySuccess.class);
  }

  @Test
  @DisplayName("listAnswers returns paged")
  void listAnswers_returnsPaged() {
    UUID qid = UUID.randomUUID();
    AnswerEntity entity = new AnswerEntity();
    entity.setId(UUID.randomUUID());
    entity.setQuestionId(qid);
    Page<AnswerEntity> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1);
    when(answerService.listByQuestion(qid, any())).thenReturn(page);
    Answer mapped = Answer.newBuilder()
        .id(entity.getId().toString()).questionId(qid.toString()).myAnswer("my").correct(true).correctAnswer("c").build();
    when(answerMapper.map(entity)).thenReturn(mapped);

    PagedAnswers result = service.listAnswers(qid.toString(), null);

    assertThat(result.getItems()).hasSize(1);
  }
}
