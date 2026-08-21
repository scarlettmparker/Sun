package com.sun.hades.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.fates.service.PersonService;
import com.sun.gaia.model.ObjectShareEntity;
import com.sun.gaia.repository.ObjectShareRepository;
import com.sun.gaia.service.AccountService;
import com.sun.gaia.service.EmailService;
import com.sun.gaia.service.PermifyService;
import com.sun.gaia.service.UserContextHolder;
import com.sun.hades.mappers.ObjectShareMapper;
import com.sun.hades.model.PrivateNoteEntity;
import com.sun.hades.repository.PrivateNoteRepository;
import com.sun.hades.repository.ReaderAccountRepository;
import com.sun.hades.repository.ReaderTextRepository;
import com.sun.hades.model.ReaderTextEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrivateNoteServiceTest {

  @Mock private PrivateNoteRepository noteRepository;
  @Mock private ReaderTextRepository textRepository;
  @Mock private ReaderAccountRepository readerAccountRepository;
  @Mock private PermifyService permifyService;
  @Mock private ObjectShareRepository shareRepository;
  @Mock private ObjectShareMapper shareMapper;
  @Mock private AccountService accountService;
  @Mock private PersonService personService;
  @Mock private EmailService emailService;

  private PrivateNoteService service;

  private final UUID viewer = UUID.randomUUID();
  private final UUID textId = UUID.randomUUID();
  private final UUID recipient = UUID.randomUUID();

  @BeforeEach
  void setUser() {
    UserContextHolder.setUserId(viewer);
    service = new PrivateNoteService(
        noteRepository, textRepository, readerAccountRepository, permifyService,
        shareRepository, shareMapper, accountService, personService, emailService);
  }

  @AfterEach
  void clearUser() {
    UserContextHolder.clear();
  }

  /**
   * Future notes inherit shares from previous notes on the same text.
   */
  @Test
  void createPrivateNote_autoSharesWithExistingRecipients() {
    when(textRepository.findById(textId)).thenReturn(Optional.of(new ReaderTextEntity()));
    PrivateNoteEntity existing = note();
    existing.setId(UUID.randomUUID());
    existing.setOwnerId(viewer);
    existing.setTextId(textId);
    when(noteRepository.findByOwnerIdAndTextId(viewer, textId)).thenReturn(List.of(existing));

    ObjectShareEntity share = new ObjectShareEntity();
    share.setObjectType("private_note");
    share.setObjectId(existing.getId());
    share.setSubjectType("user");
    share.setSubjectId(recipient);
    share.setRelation("VIEWER");
    when(shareRepository.findByObjectTypeAndObjectIdIn(eq("private_note"), any())).thenReturn(List.of(share));

    PrivateNoteEntity saved = note();
    saved.setId(UUID.randomUUID());
    when(noteRepository.save(any())).thenReturn(saved);
    when(shareRepository.findByObjectTypeAndObjectId(eq("private_note"), any())).thenReturn(List.of());
    when(shareMapper.toEntity(eq("private_note"), any(), eq("user"), eq(recipient), eq("VIEWER")))
        .thenReturn(share);

    UUID newId = service.createPrivateNote(textId, 0, 10, "new body");

    assertThat(newId).isEqualTo(saved.getId());
    verify(shareRepository).saveAll(any());
    verify(permifyService).writeTuples(any());
  }

  /**
   * First note on a text has no recipients to auto-share.
   */
  @Test
  void createPrivateNote_noAutoShareWhenNoRecipients() {
    when(textRepository.findById(textId)).thenReturn(Optional.of(new ReaderTextEntity()));
    when(noteRepository.findByOwnerIdAndTextId(viewer, textId)).thenReturn(List.of());
    PrivateNoteEntity saved = note();
    saved.setId(UUID.randomUUID());
    when(noteRepository.save(any())).thenReturn(saved);

    UUID newId = service.createPrivateNote(textId, 0, 10, "first");

    assertThat(newId).isEqualTo(saved.getId());
  }

  private PrivateNoteEntity note() {
    PrivateNoteEntity n = new PrivateNoteEntity();
    n.setOwnerId(viewer);
    n.setTextId(textId);
    n.setStartOffset(0);
    n.setEndOffset(10);
    n.setBody("body");
    return n;
  }
}
