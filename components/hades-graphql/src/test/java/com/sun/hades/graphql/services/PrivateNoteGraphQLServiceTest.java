package com.sun.hades.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.hades.codegen.types.PagedPrivateNotes;
import com.sun.hades.codegen.types.PrivateNote;
import com.sun.hades.codegen.types.PrivateNoteInput;
import com.sun.hades.codegen.types.QuerySuccess;
import com.sun.hades.codegen.types.ShareNotesInput;
import com.sun.hades.codegen.types.StandardError;
import com.sun.hades.graphql.mappers.PrivateNoteMapper;
import com.sun.hades.graphql.mappers.RemoteUserMapper;
import com.sun.hades.model.PrivateNoteEntity;
import com.sun.hades.service.PrivateNoteService;
import com.sun.hades.service.ReaderAccountService;
import java.util.List;
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
    when(privateNoteService.createPrivateNote(eq(textId), eq(0), eq(10), eq("note"))).thenReturn(returned);

    var result = service.createPrivateNote(input);

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(returned.toString());
  }

  @Test
  void createPrivateNote_returnsStandardErrorOnFailure() {
    UUID textId = UUID.randomUUID();
    PrivateNoteInput input = PrivateNoteInput.newBuilder()
        .textId(textId.toString()).startOffset(0).endOffset(10).body("").build();
    when(privateNoteService.createPrivateNote(any(), any(int.class), any(int.class), any()))
        .thenThrow(new IllegalArgumentException("Invalid private note"));

    var result = service.createPrivateNote(input);

    assertThat(result).isInstanceOf(StandardError.class);
    assertThat(((StandardError) result).getMessage()).contains("Invalid private note");
  }

  @Test
  void deletePrivateNote_delegates() {
    UUID id = UUID.randomUUID();

    var result = service.deletePrivateNote(id.toString());

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(id.toString());
    verify(privateNoteService).deletePrivateNote(id);
  }

  @Test
  void deletePrivateNote_returnsStandardErrorWhenThrows() {
    UUID id = UUID.randomUUID();
    org.mockito.Mockito.doThrow(new IllegalArgumentException("Not the owner"))
        .when(privateNoteService).deletePrivateNote(id);

    var result = service.deletePrivateNote(id.toString());

    assertThat(result).isInstanceOf(StandardError.class);
  }

  @Test
  void shareNotes_delegates() {
    UUID textId = UUID.randomUUID();
    UUID subjectId = UUID.randomUUID();
    ShareNotesInput input = ShareNotesInput.newBuilder()
        .textId(textId.toString()).subjectIds(List.of(subjectId.toString())).build();
    when(privateNoteService.shareNotes(eq(textId), any(), any())).thenReturn(textId);

    var result = service.shareNotes(input);

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(textId.toString());
  }

  @Test
  void shareNotes_returnsStandardErrorOnFailure() {
    UUID textId = UUID.randomUUID();
    ShareNotesInput input = ShareNotesInput.newBuilder()
        .textId(textId.toString()).subjectIds(List.of()).build();
    when(privateNoteService.shareNotes(any(), any(), any()))
        .thenThrow(new IllegalArgumentException("Text not found"));

    var result = service.shareNotes(input);

    assertThat(result).isInstanceOf(StandardError.class);
  }
}
