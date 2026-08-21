package com.sun.hades.service;

import com.sun.base.service.BaseService;
import com.sun.fates.service.PersonService;
import com.sun.gaia.model.ObjectShareEntity;
import com.sun.gaia.model.enums.AccountType;
import com.sun.gaia.repository.ObjectShareRepository;
import com.sun.hades.mappers.ObjectShareMapper;
import com.sun.gaia.service.AccountService;
import com.sun.gaia.service.EmailService;
import com.sun.gaia.service.PermifyService;
import com.sun.gaia.service.UserContextHolder;
import com.sun.hades.model.PrivateNoteEntity;
import com.sun.hades.model.enums.PrivateNoteVisibility;
import com.sun.hades.repository.PrivateNoteRepository;
import com.sun.hades.repository.ReaderAccountRepository;
import com.sun.hades.repository.ReaderTextRepository;
import com.sun.hades.service.RemoteObjectReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for per-user private notes anchored to text ranges.
 */
@Service
@Transactional
public class PrivateNoteService extends BaseService<PrivateNoteEntity> {

  private static final Logger logger = LoggerFactory.getLogger(PrivateNoteService.class);

  private final PrivateNoteRepository noteRepository;
  private final ReaderTextRepository textRepository;
  private final ReaderAccountRepository readerAccountRepository;
  private final PermifyService permifyService;
  private final ObjectShareRepository shareRepository;
  private final ObjectShareMapper shareMapper;
  private final AccountService accountService;
  private final PersonService personService;
  private final EmailService emailService;

  public PrivateNoteService(
      PrivateNoteRepository repository,
      ReaderTextRepository textRepository,
      ReaderAccountRepository readerAccountRepository,
      PermifyService permifyService,
      ObjectShareRepository shareRepository,
      ObjectShareMapper shareMapper,
      AccountService accountService,
      PersonService personService,
      EmailService emailService) {
    super(repository);
    this.noteRepository = repository;
    this.textRepository = textRepository;
    this.readerAccountRepository = readerAccountRepository;
    this.permifyService = permifyService;
    this.shareRepository = shareRepository;
    this.shareMapper = shareMapper;
    this.accountService = accountService;
    this.personService = personService;
    this.emailService = emailService;
  }

  /**
   * Creates a private note on a range.
   *
   * @param textId the text id
   * @param startOffset the range start
   * @param endOffset the range end
   * @param body the markdown body
   * @return the new note id
   */
  public UUID createPrivateNote(UUID textId, int startOffset, int endOffset, String body) {
    UUID viewer = requireUser();
    if (startOffset < 0 || endOffset <= startOffset || body == null || body.isBlank()) {
      throw new IllegalArgumentException("Invalid private note");
    }
    textRepository
        .findById(textId)
        .orElseThrow(() -> new IllegalArgumentException("Text not found: " + textId));

    Set<UUID> existingRecipients = findExistingRecipients(textId, viewer);

    PrivateNoteEntity note = new PrivateNoteEntity();
    note.setOwnerId(viewer);
    note.setTextId(textId);
    note.setStartOffset(startOffset);
    note.setEndOffset(endOffset);
    note.setBody(body);
    note.setVisibility(PrivateNoteVisibility.PRIVATE);
    note.setRemoteObject(List.of("private_note", "hades:text:" + textId));
    UUID noteId = noteRepository.save(note).getId();

    if (!existingRecipients.isEmpty()) {
      createSharesForNote(noteId, existingRecipients);
    }

    return noteId;
  }

  /**
   * Lists private notes for a text visible to the current viewer.
   *
   * @param textId the text id
   * @param pageable the page request
   * @return the page of notes
   */
  @Transactional(readOnly = true)
  public Page<PrivateNoteEntity> listForText(UUID textId, Pageable pageable) {
    UUID viewer = requireUser();
    List<PrivateNoteEntity> all = noteRepository.findByTextId(textId);
    List<PrivateNoteEntity> visible = new ArrayList<>();
    for (PrivateNoteEntity n : all) {
      if (canView(viewer, n)) {
        visible.add(n);
      }
    }
    int start = Math.min((int) pageable.getOffset(), visible.size());
    int end = Math.min(start + pageable.getPageSize(), visible.size());
    List<PrivateNoteEntity> content = visible.subList(start, end);
    return new PageImpl<>(content, pageable, visible.size());
  }

  /**
   * Deletes a private note (owner only).
   *
   * @param id the note id
   */
  public void deletePrivateNote(UUID id) {
    UUID viewer = requireUser();
    PrivateNoteEntity note =
        noteRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Private note not found: " + id));
    if (!note.getOwnerId().equals(viewer)) {
      throw new IllegalArgumentException("Not the owner");
    }
    noteRepository.deleteById(id);
  }

  /**
   * Shares all notes on a text.
   *
   * @param textId the text id
   * @param subjectIds the subject account ids
   * @param subjectEmails the subject emails
   * @return the text id
   */
  public UUID shareNotes(UUID textId, List<UUID> subjectIds, List<String> subjectEmails) {
    UUID viewer = requireUser();
    var text = textRepository.findById(textId)
        .orElseThrow(() -> new IllegalArgumentException("Text not found: " + textId));
    Set<UUID> subjects = resolveSubjects(subjectIds, subjectEmails, viewer);
    if (subjects.isEmpty()) {
      return textId;
    }
    List<PrivateNoteEntity> notes = noteRepository.findByOwnerIdAndTextId(viewer, textId);
    if (notes.isEmpty()) {
      return textId;
    }
    createShares(notes, subjects);
    String firstNoteId = notes.get(0).getId().toString();
    sendShareEmails(subjects, text.getTitle(), viewer, textId.toString(), firstNoteId);
    return textId;
  }

  /**
   * Resolves ids and emails to human account ids.
   *
   * @param subjectIds the account ids
   * @param subjectEmails the emails
   * @param viewer the viewer id
   * @return the human subjects
   */
  private Set<UUID> resolveSubjects(List<UUID> subjectIds, List<String> subjectEmails, UUID viewer) {
    Set<UUID> subjects = new HashSet<>();
    if (subjectIds != null) {
      List<UUID> filtered = subjectIds.stream()
          .filter(id -> id != null && !id.equals(viewer))
          .toList();
      for (UUID sid : filtered) {
        var account = accountService.findById(sid)
            .orElseThrow(() -> new IllegalArgumentException("Invalid subject: " + sid));
        if (account.getAccountType() != AccountType.HUMAN) {
          throw new IllegalArgumentException("Invalid subject: " + sid);
        }
        subjects.add(sid);
      }
    }
    if (subjectEmails != null) {
      for (String email : subjectEmails) {
        if (email == null || email.isBlank()) {
          continue;
        }
        String trimmed = email.trim().toLowerCase();
        List<UUID> resolved = accountService.findByPersonEmail(trimmed).stream()
            .filter(a -> a.getAccountType() == AccountType.HUMAN)
            .map(a -> a.getId())
            .filter(id -> !id.equals(viewer))
            .toList();
        subjects.addAll(resolved);
      }
    }
    return subjects;
  }

  /**
   * Creates viewer shares for each note.
   *
   * @param notes the notes to share
   * @param subjects the subject ids
   */
  private void createShares(List<PrivateNoteEntity> notes, Set<UUID> subjects) {
    if (notes.isEmpty() || subjects.isEmpty()) {
      return;
    }
    List<UUID> noteIds = notes.stream().map(PrivateNoteEntity::getId).toList();
    List<ObjectShareEntity> existing = shareRepository.findByObjectTypeAndObjectIdIn("private_note", noteIds);
    Set<String> existingKeys = new HashSet<>();
    for (ObjectShareEntity share : existing) {
      existingKeys.add(share.getObjectId() + ":" + share.getSubjectId());
    }
    List<ObjectShareEntity> toSave = new ArrayList<>();
    List<Map<String, String>> tuples = new ArrayList<>();
    for (PrivateNoteEntity note : notes) {
      for (UUID subjectId : subjects) {
        String key = note.getId() + ":" + subjectId;
        if (existingKeys.contains(key)) {
          continue;
        }
        toSave.add(shareMapper.toEntity("private_note", note.getId(), "user", subjectId, "VIEWER"));
        Map<String, String> tuple = new HashMap<>();
        tuple.put("object", "private_note:" + note.getId());
        tuple.put("relation", "viewer");
        tuple.put("subject", "user:" + subjectId);
        tuples.add(tuple);
        existingKeys.add(key);
      }
    }
    if (!toSave.isEmpty()) {
      shareRepository.saveAll(toSave);
    }
    if (!tuples.isEmpty()) {
      try {
        permifyService.writeTuples(tuples);
      } catch (Exception e) {
        logger.error("Failed to write permify tuples for {} shares", tuples.size(), e);
      }
    }
  }

  /**
   * Finds recipients already shared on this text.
   *
   * @param textId the text id
   * @param viewer the owner id
   * @return the recipient ids
   */
  private Set<UUID> findExistingRecipients(UUID textId, UUID viewer) {
    List<PrivateNoteEntity> notes = noteRepository.findByOwnerIdAndTextId(viewer, textId);
    if (notes.isEmpty()) {
      return new HashSet<>();
    }
    List<UUID> noteIds = notes.stream().map(PrivateNoteEntity::getId).toList();
    List<ObjectShareEntity> shares = shareRepository.findByObjectTypeAndObjectIdIn("private_note", noteIds);
    Set<UUID> recipients = new HashSet<>();
    for (ObjectShareEntity share : shares) {
      if ("user".equals(share.getSubjectType()) && "VIEWER".equals(share.getRelation())) {
        recipients.add(share.getSubjectId());
      }
    }
    return recipients;
  }

  /**
   * Creates shares for a single note without email.
   *
   * @param noteId the note id
   * @param subjects the recipients
   */
  private void createSharesForNote(UUID noteId, Set<UUID> subjects) {
    if (subjects.isEmpty()) {
      return;
    }
    List<ObjectShareEntity> existing = shareRepository.findByObjectTypeAndObjectId("private_note", noteId);
    Set<UUID> existingSubjects = new HashSet<>();
    for (ObjectShareEntity share : existing) {
      if ("user".equals(share.getSubjectType()) && "VIEWER".equals(share.getRelation())) {
        existingSubjects.add(share.getSubjectId());
      }
    }
    List<ObjectShareEntity> toSave = new ArrayList<>();
    List<Map<String, String>> tuples = new ArrayList<>();
    for (UUID subjectId : subjects) {
      if (existingSubjects.contains(subjectId)) {
        continue;
      }
      toSave.add(shareMapper.toEntity("private_note", noteId, "user", subjectId, "VIEWER"));
      Map<String, String> tuple = new HashMap<>();
      tuple.put("object", "private_note:" + noteId);
      tuple.put("relation", "viewer");
      tuple.put("subject", "user:" + subjectId);
      tuples.add(tuple);
    }
    if (!toSave.isEmpty()) {
      shareRepository.saveAll(toSave);
    }
    if (!tuples.isEmpty()) {
      try {
        permifyService.writeTuples(tuples);
      } catch (Exception e) {
        logger.error("Failed to write permify tuples for note {}", noteId, e);
      }
    }
  }

  /**
   * Sends one email per recipient.
   *
   * @param subjects the recipients
   * @param textTitle the text title
   * @param viewer the sharer id
   * @param textId the text id
   * @param noteId the first note id for the link
   */
  private void sendShareEmails(Set<UUID> subjects, String textTitle, UUID viewer, String textId, String noteId) {
    String sharerName = readerAccountRepository.findByGaiaAccountId(viewer)
        .map(r -> {
          if (r.getGlobalName() != null && !r.getGlobalName().isBlank()) {
            return r.getGlobalName();
          }
          if (r.getDiscordUsername() != null && !r.getDiscordUsername().isBlank()) {
            return r.getDiscordUsername();
          }
          return null;
        })
        .filter(s -> s != null && !s.isBlank())
        .orElseGet(() -> accountService.findById(viewer).map(a -> a.getUsername()).orElse("Someone"));
    for (UUID subjectId : subjects) {
      try {
        String toEmail = accountService.findById(subjectId)
            .flatMap(a -> personService.findById(a.getPersonId()))
            .map(p -> p.getEmail())
            .orElse(null);
        if (toEmail != null && !toEmail.isBlank()) {
          emailService.sendShareNotesEmail(toEmail, textTitle, sharerName, textId, noteId);
        }
      } catch (Exception e) {
        logger.error("Failed to send share email to subject {}", subjectId, e);
      }
    }
  }

  /**
   * Finds notes referencing any of the given remote object ids, filtered by viewer.
   *
   * @param ids the remote object ids
   * @return the matching references
   */
  @Transactional(readOnly = true)
  public List<RemoteObjectReference> locateRemoteObjects(List<String> ids) {
    UUID viewer = UserContextHolder.getUserId();
    if (viewer == null) {
      return List.of();
    }
    String[] arr = ids.toArray(new String[0]);
    List<RemoteObjectReference> out = new ArrayList<>();
    for (PrivateNoteEntity n : noteRepository.findByRemoteObjectsIn(arr)) {
      if (canView(viewer, n)) {
        out.add(new RemoteObjectReference(n.getId(), "PRIVATE_NOTE", n.getId(), null));
      }
    }
    return out;
  }

  /**
   * Checks whether the viewer may see the note.
   *
   * @param viewer the viewer id
   * @param note the note
   * @return true when visible
   */
  private boolean canView(UUID viewer, PrivateNoteEntity note) {
    if (note.getOwnerId().equals(viewer)) {
      return true;
    }
    return permifyService.check("user:" + viewer, "view", "private_note:" + note.getId());
  }

  /**
   * Returns the authenticated user or throws.
   *
   * @return the caller's account id
   */
  private UUID requireUser() {
    UUID id = UserContextHolder.getUserId();
    if (id == null) {
      throw new IllegalArgumentException("Authentication required");
    }
    return id;
  }
}
