package com.sun.hades.graphql.services;

import com.sun.base.error.MutationException;
import com.sun.hades.codegen.types.CreatePrivateNoteResponse;
import com.sun.hades.codegen.types.DeletePrivateNoteResponse;
import com.sun.hades.codegen.types.PagedPrivateNotes;
import com.sun.hades.codegen.types.PaginationInput;
import com.sun.hades.codegen.types.PrivateNote;
import com.sun.hades.codegen.types.PrivateNoteInput;
import com.sun.hades.codegen.types.RemoteUser;
import com.sun.hades.codegen.types.ShareNotesInput;
import com.sun.hades.codegen.types.ShareNotesResponse;
import com.sun.hades.graphql.mappers.PrivateNoteMapper;
import com.sun.hades.graphql.mappers.RemoteUserMapper;
import com.sun.hades.model.PrivateNoteEntity;
import com.sun.hades.service.PrivateNoteService;
import com.sun.hades.service.ReaderAccountService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL business logic for the reader.
 */
@Service
public class PrivateNoteGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(PrivateNoteGraphQLService.class);

  private final PrivateNoteService privateNoteService;
  private final ReaderAccountService accountService;
  private final PrivateNoteMapper privateNoteMapper;
  private final RemoteUserMapper remoteUserMapper;

  public PrivateNoteGraphQLService(PrivateNoteService privateNoteService,
      ReaderAccountService accountService, PrivateNoteMapper privateNoteMapper,
      RemoteUserMapper remoteUserMapper) {
    this.privateNoteService = privateNoteService;
    this.accountService = accountService;
    this.privateNoteMapper = privateNoteMapper;
    this.remoteUserMapper = remoteUserMapper;
  }

  /**
   * Paginated private notes for a text, visible to the current viewer.
   *
   * @param textId the text id
   * @param pagination the page request
   * @return the paged private notes
   */
  @Transactional(readOnly = true)
  public PagedPrivateNotes privateNotes(String textId, PaginationInput pagination) {
    UUID id = UUID.fromString(textId);
    Pageable pageable = HadesGraphQLSupport.toPageable(pagination, "createdAt", Sort.Direction.DESC);
    var page = privateNoteService.listForText(id, pageable);
    Map<UUID, RemoteUser> authors = new HashMap<>();
    List<UUID> ownerIds = page.getContent().stream()
        .map(PrivateNoteEntity::getOwnerId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    accountService.findByGaiaAccountIdIn(ownerIds).forEach(acc ->
        authors.put(acc.getGaiaAccountId(), remoteUserMapper.discord(acc.getDiscordId())));
    List<PrivateNote> items = page.getContent().stream()
        .map(n -> privateNoteMapper.map(n, authors.get(n.getOwnerId())))
        .toList();
    return PagedPrivateNotes.newBuilder().items(items).pageInfo(HadesGraphQLSupport.pageInfo(page)).build();
  }

  /**
   * Creates a private note on a range.
   *
   * @param input the private note input
   * @return the create-private-note response
   */
  @Transactional
  public CreatePrivateNoteResponse createPrivateNote(PrivateNoteInput input) {
    try {
      UUID id = privateNoteService.createPrivateNote(
          UUID.fromString(input.getTextId()),
          input.getStartOffset(),
          input.getEndOffset(),
          input.getBody());
      logger.info("createPrivateNote succeeded for id {}", id);
      PrivateNoteEntity note = privateNoteService.findById(id)
          .orElseThrow(() -> new IllegalArgumentException("Private note not found: " + id));
      return CreatePrivateNoteResponse.newBuilder()
          .message("Private note created successfully")
          .note(privateNoteMapper.map(note, null))
          .build();
    } catch (Exception e) {
      logger.error("createPrivateNote failed", e);
      throw new MutationException("createPrivateNote failed: " + e.getMessage(), e);
    }
  }

  /**
   * Deletes a private note (owner only).
   *
   * @param id the note id
   * @return the delete-private-note response
   */
  @Transactional
  public DeletePrivateNoteResponse deletePrivateNote(String id) {
    try {
      privateNoteService.deletePrivateNote(UUID.fromString(id));
      logger.info("deletePrivateNote succeeded for id {}", id);
      return DeletePrivateNoteResponse.newBuilder()
          .message("Private note deleted successfully")
          .id(id)
          .build();
    } catch (Exception e) {
      logger.error("deletePrivateNote failed", e);
      throw new MutationException("deletePrivateNote failed: " + e.getMessage(), e);
    }
  }

  /**
   * Shares all private notes on a text.
   *
   * @param input the share input
   * @return the share-notes response
   */
  @Transactional
  public ShareNotesResponse shareNotes(ShareNotesInput input) {
    try {
      UUID id = privateNoteService.shareNotes(
          UUID.fromString(input.getTextId()),
          input.getSubjectIds() == null ? List.of() : input.getSubjectIds().stream()
              .map(s -> {
                try {
                  return UUID.fromString(s);
                } catch (Exception e) {
                  return null;
                }
              })
              .filter(s -> s != null)
              .toList(),
          input.getSubjectEmails() == null ? List.of() : input.getSubjectEmails());
      logger.info("shareNotes succeeded for id {}", id);
      return ShareNotesResponse.newBuilder()
          .message("Notes shared successfully")
          .id(id.toString())
          .build();
    } catch (Exception e) {
      logger.error("shareNotes failed", e);
      throw new MutationException("shareNotes failed: " + e.getMessage(), e);
    }
  }
}
