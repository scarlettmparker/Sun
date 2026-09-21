package com.sun.hades.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.base.error.MutationException;
import com.sun.hades.codegen.types.CreatePrivateNoteResponse;
import com.sun.hades.codegen.types.DeletePrivateNoteResponse;
import com.sun.hades.codegen.types.PagedPrivateNotes;
import com.sun.hades.codegen.types.PrivateNote;
import com.sun.hades.codegen.types.PrivateNoteInput;
import com.sun.hades.codegen.types.ShareNotesInput;
import com.sun.hades.codegen.types.ShareNotesResponse;
import com.sun.hades.graphql.mappers.PrivateNoteMapper;
import com.sun.hades.graphql.mappers.RemoteUserMapper;
import com.sun.hades.model.PrivateNoteEntity;
import com.sun.hades.service.PrivateNoteService;
import com.sun.hades.service.ReaderAccountService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class PrivateNoteGraphQLServiceTest {

  @Mock private PrivateNoteService privateNoteService;
  @Mock private ReaderAccountService accountService;
  @Mock private PrivateNoteMapper privateNoteMapper;
  @Mock private RemoteUserMapper remoteUserMapper;

  @InjectMocks private PrivateNoteGraphQLService service;

  @Test
  void privateNotes_returnsPaged() {
    UUID textId = UUID.randomUUID();
    PrivateNoteEntity entity = new PrivateNoteEntity();
    entity.setId(UUID.randomUUID());
    entity.setOwnerId(UUID.randomUUID());
    entity.setTextId(textId);
    entity.setBody("body");
    Page<PrivateNoteEntity> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 10), 1);
    when(privateNoteService.listForText(eq(textId), any())).thenReturn(page);
    PrivateNote mapped = PrivateNote.newBuilder().id(entity.getId().toString()).body("body").build();
    when(privateNoteMapper.map(eq(entity), any())).thenReturn(mapped);

    PagedPrivateNotes result = service.privateNotes(textId.toString(), null);

    assertThat(result.getItems()).hasSize(1);
    assertThat(result.getItems().get(0).getBody()).isEqualTo("body");
    assertThat(result.getPageInfo().getTotalCount()).isEqualTo(1);
  }

  @Test
  void createPrivateNote_delegates() {
    UUID textId = UUID.randomUUID();
    UUID returned = UUID.randomUUID();
    PrivateNoteInput input = PrivateNoteInput.newBuilder()
        .textId(textId.toString()).startOffset(0).endOffset(10).body("note").build();
    PrivateNoteEntity entity = new PrivateNoteEntity();
    entity.setId(returned);
    PrivateNote mapped = PrivateNote.newBuilder().id(returned.toString()).body("note").build();
    when(privateNoteService.createPrivateNote(eq(textId), eq(0), eq(10), eq("note"))).thenReturn(returned);
    when(privateNoteService.findById(returned)).thenReturn(Optional.of(entity));
    when(privateNoteMapper.map(eq(entity), any())).thenReturn(mapped);

    CreatePrivateNoteResponse result = service.createPrivateNote(input);

    assertThat(result.getNote()).isEqualTo(mapped);
  }

  @Test
  void createPrivateNote_throwsOnFailure() {
    UUID textId = UUID.randomUUID();
    PrivateNoteInput input = PrivateNoteInput.newBuilder()
        .textId(textId.toString()).startOffset(0).endOffset(10).body("").build();
    when(privateNoteService.createPrivateNote(any(), any(int.class), any(int.class), any()))
        .thenThrow(new IllegalArgumentException("Invalid private note"));

    assertThatThrownBy(() -> service.createPrivateNote(input))
        .isInstanceOf(MutationException.class)
        .hasMessageContaining("Invalid private note");
  }

  @Test
  void deletePrivateNote_delegates() {
    UUID id = UUID.randomUUID();

    DeletePrivateNoteResponse result = service.deletePrivateNote(id.toString());

    assertThat(result.getId()).isEqualTo(id.toString());
    verify(privateNoteService).deletePrivateNote(id);
  }

  @Test
  void deletePrivateNote_throwsWhenThrows() {
    UUID id = UUID.randomUUID();
    org.mockito.Mockito.doThrow(new IllegalArgumentException("Not the owner"))
        .when(privateNoteService).deletePrivateNote(id);

    assertThatThrownBy(() -> service.deletePrivateNote(id.toString()))
        .isInstanceOf(MutationException.class);
  }

  @Test
  void shareNotes_delegates() {
    UUID textId = UUID.randomUUID();
    UUID subjectId = UUID.randomUUID();
    ShareNotesInput input = ShareNotesInput.newBuilder()
        .textId(textId.toString()).subjectIds(List.of(subjectId.toString())).build();
    when(privateNoteService.shareNotes(eq(textId), any(), any())).thenReturn(textId);

    ShareNotesResponse result = service.shareNotes(input);

    assertThat(result.getId()).isEqualTo(textId.toString());
  }

  @Test
  void shareNotes_throwsOnFailure() {
    UUID textId = UUID.randomUUID();
    ShareNotesInput input = ShareNotesInput.newBuilder()
        .textId(textId.toString()).subjectIds(List.of()).build();
    when(privateNoteService.shareNotes(any(), any(), any()))
        .thenThrow(new IllegalArgumentException("Text not found"));

    assertThatThrownBy(() -> service.shareNotes(input))
        .isInstanceOf(MutationException.class);
  }
}
