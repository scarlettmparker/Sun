package com.sun.hades.service;

import com.sun.base.service.BaseService;
import com.sun.gaia.service.PermifyService;
import com.sun.gaia.service.UserContextHolder;
import com.sun.hades.model.PrivateNoteEntity;
import com.sun.hades.model.enums.PrivateNoteVisibility;
import com.sun.hades.repository.PrivateNoteRepository;
import com.sun.hades.repository.ReaderTextRepository;
import com.sun.hades.service.RemoteObjectReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

  private final PrivateNoteRepository noteRepository;
  private final ReaderTextRepository textRepository;
  private final PermifyService permifyService;

  public PrivateNoteService(
      PrivateNoteRepository repository,
      ReaderTextRepository textRepository,
      PermifyService permifyService) {
    super(repository);
    this.noteRepository = repository;
    this.textRepository = textRepository;
    this.permifyService = permifyService;
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

    PrivateNoteEntity note = new PrivateNoteEntity();
    note.setOwnerId(viewer);
    note.setTextId(textId);
    note.setStartOffset(startOffset);
    note.setEndOffset(endOffset);
    note.setBody(body);
    note.setVisibility(PrivateNoteVisibility.PRIVATE);
    note.setRemoteObject(List.of("private_note", "hades:text:" + textId));
    return noteRepository.save(note).getId();
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
