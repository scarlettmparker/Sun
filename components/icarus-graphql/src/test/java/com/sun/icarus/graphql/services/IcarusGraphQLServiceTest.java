package com.sun.icarus.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sun.icarus.codegen.types.CreateThreadInput;
import com.sun.icarus.codegen.types.ForumThread;
import com.sun.icarus.codegen.types.QueryResult;
import com.sun.icarus.codegen.types.QuerySuccess;
import com.sun.icarus.graphql.mappers.ForumPostMapper;
import com.sun.icarus.graphql.mappers.ForumThreadMapper;
import com.sun.icarus.model.ForumThreadEntity;
import com.sun.icarus.service.ForumPostService;
import com.sun.icarus.service.ForumThreadService;
import com.sun.icarus.service.ForumVoteService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IcarusGraphQLServiceTest {

  @Mock private ForumThreadService threadService;
  @Mock private ForumPostService postService;
  @Mock private ForumVoteService voteService;
  @Mock private ForumThreadMapper threadMapper;
  @Mock private ForumPostMapper postMapper;

  @InjectMocks private IcarusGraphQLService service;

  @Test
  void threadsFor_shouldReturnMappedThreads() {
    ForumThreadEntity entity = new ForumThreadEntity();
    entity.setId(UUID.randomUUID());
    entity.setTitle("Thread");
    when(threadService.listForRemoteObject("hades:annotation:abc")).thenReturn(List.of(entity));
    ForumThread mapped = ForumThread.newBuilder().id(entity.getId().toString()).title("Thread").build();
    when(threadMapper.map(entity)).thenReturn(mapped);

    List<ForumThread> result = service.threadsFor("hades:annotation:abc").getItems();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTitle()).isEqualTo("Thread");
  }

  @Test
  void createThread_shouldReturnQuerySuccess() {
    UUID id = UUID.randomUUID();
    when(threadService.create("Thread", "hades:annotation:abc")).thenReturn(id);

    QueryResult result = service.createThread(CreateThreadInput.newBuilder()
        .title("Thread").remoteObject("hades:annotation:abc").build());

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(id.toString());
  }

  @Test
  void createThread_shouldReturnStandardErrorWhenServiceThrows() {
    when(threadService.create(any(), any())).thenThrow(new RuntimeException("Database error"));

    QueryResult result = service.createThread(CreateThreadInput.newBuilder()
        .title("Thread").remoteObject("hades:annotation:abc").build());

    assertThat(result).isInstanceOf(com.sun.icarus.codegen.types.StandardError.class);
    assertThat(((com.sun.icarus.codegen.types.StandardError) result).getMessage()).contains("Database error");
  }
}
