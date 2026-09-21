package com.sun.icarus.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sun.base.error.MutationException;
import com.sun.icarus.codegen.types.CreateThreadInput;
import com.sun.icarus.codegen.types.CreateThreadResponse;
import com.sun.icarus.codegen.types.ForumThread;
import com.sun.icarus.graphql.mappers.ForumPostMapper;
import com.sun.icarus.graphql.mappers.ForumThreadMapper;
import com.sun.icarus.model.ForumThreadEntity;
import com.sun.icarus.service.ForumPostService;
import com.sun.icarus.service.ForumThreadService;
import com.sun.icarus.service.ForumVoteService;
import java.util.List;
import java.util.Optional;
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
  @Mock private com.sun.gaia.service.AccountService accountService;

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
  void createThread_shouldReturnThread() {
    UUID id = UUID.randomUUID();
    ForumThreadEntity entity = new ForumThreadEntity();
    entity.setId(id);
    entity.setTitle("Thread");
    ForumThread mapped = ForumThread.newBuilder().id(id.toString()).title("Thread").build();
    when(threadService.create("Thread", "hades:annotation:abc")).thenReturn(id);
    when(threadService.findById(id)).thenReturn(Optional.of(entity));
    when(threadMapper.map(entity)).thenReturn(mapped);

    CreateThreadResponse result = service.createThread(CreateThreadInput.newBuilder()
        .title("Thread").remoteObject("hades:annotation:abc").build());

    assertThat(result.getThread()).isEqualTo(mapped);
    assertThat(result.getMessage()).isEqualTo("createThread succeeded");
  }

  @Test
  void createThread_shouldThrowMutationExceptionWhenServiceThrows() {
    when(threadService.create(any(), any())).thenThrow(new RuntimeException("Database error"));

    assertThatThrownBy(() -> service.createThread(CreateThreadInput.newBuilder()
        .title("Thread").remoteObject("hades:annotation:abc").build()))
        .isInstanceOf(MutationException.class)
        .hasMessageContaining("Database error");
  }
}
