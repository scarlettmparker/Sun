package com.sun.icarus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.service.UserContextHolder;
import com.sun.icarus.model.ForumThreadEntity;
import com.sun.icarus.repository.ForumThreadRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ForumThreadServiceTest {

  @Mock private ForumThreadRepository threadRepository;

  @InjectMocks private ForumThreadService service;

  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUser() {
    UserContextHolder.setUserId(userId);
  }

  @AfterEach
  void clearUser() {
    UserContextHolder.clear();
  }

  @Test
  void create_storesRemoteObject() {
    ForumThreadEntity saved = new ForumThreadEntity();
    saved.setId(UUID.randomUUID());
    when(threadRepository.save(any())).thenReturn(saved);

    service.create("Title", "hades:annotation:abc");

    ArgumentCaptor<ForumThreadEntity> captor = ArgumentCaptor.forClass(ForumThreadEntity.class);
    verify(threadRepository).save(captor.capture());
    assertThat(captor.getValue().getRemoteObject()).containsExactly("hades:annotation:abc");
  }

  @Test
  void listForRemoteObject_delegatesToRepository() {
    ForumThreadEntity thread = new ForumThreadEntity();
    thread.setId(UUID.randomUUID());
    when(threadRepository.findByRemoteObjectsIn(new String[] {"hades:annotation:abc"}))
        .thenReturn(List.of(thread));

    List<ForumThreadEntity> result = service.listForRemoteObject("hades:annotation:abc");

    assertThat(result).extracting(ForumThreadEntity::getId).contains(thread.getId());
  }

  @Test
  void locateRemoteObjects_tagsOwnerType() {
    ForumThreadEntity thread = new ForumThreadEntity();
    thread.setId(UUID.randomUUID());
    when(threadRepository.findByRemoteObjectsIn(any(String[].class))).thenReturn(List.of(thread));

    List<RemoteObjectReference> result = service.locateRemoteObjects(List.of("hades:annotation:abc"));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).ownerType()).isEqualTo("THREAD");
  }

  @Test
  void attach_appendsTarget() {
    ForumThreadEntity thread = new ForumThreadEntity();
    thread.setId(UUID.randomUUID());
    thread.setRemoteObject(new java.util.ArrayList<>(List.of("hades:annotation:abc")));
    when(threadRepository.findById(thread.getId())).thenReturn(Optional.of(thread));
    when(threadRepository.save(any())).thenReturn(thread);

    service.attach(thread.getId(), "echo:entry:def");

    assertThat(thread.getRemoteObject()).containsExactly("hades:annotation:abc", "echo:entry:def");
  }
}
