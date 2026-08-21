package com.sun.hades.graphql.services;

import com.sun.hades.codegen.types.PagedPrivateNotes;
import com.sun.hades.codegen.types.PaginationInput;
import com.sun.hades.codegen.types.PrivateNote;
import com.sun.hades.codegen.types.PrivateNoteInput;
import com.sun.hades.codegen.types.QueryResult;
import com.sun.hades.codegen.types.QuerySuccess;
import com.sun.hades.codegen.types.RemoteUser;
import com.sun.hades.codegen.types.ShareNotesInput;
import com.sun.hades.codegen.types.StandardError;
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
import java.util.function.Supplier;
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
   * @return a QueryResult
   */
  @Transactional
  public QueryResult createPrivateNote(PrivateNoteInput input) {
    return mutate("createPrivateNote", () -> privateNoteService.createPrivateNote(
        UUID.fromString(input.getTextId()),
        input.getStartOffset(),
        input.getEndOffset(),
        input.getBody()));
  }

  /**
   * Deletes a private note (owner only).
   *
   * @param id the note id
   * @return a QueryResult
   */
  @Transactional
  public QueryResult deletePrivateNote(String id) {
    return mutate("deletePrivateNote", () -> {
      privateNoteService.deletePrivateNote(UUID.fromString(id));
      return UUID.fromString(id);
    });
  }

  /**
   * Shares all private notes on a text.
   *
   * @param input the share input
   * @return a QueryResult
   */
  @Transactional
  public QueryResult shareNotes(ShareNotesInput input) {
    return mutate("shareNotes", () -> privateNoteService.shareNotes(
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
        input.getSubjectEmails() == null ? List.of() : input.getSubjectEmails()));
  }

  /**
   * Runs a mutation, returning QuerySuccess with the affected id or StandardError
   * on failure.
   *
   * @param op the operation name (for logging and messages)
   * @param action the mutation, returning the affected entity id
   * @return a QueryResult
   */
  private QueryResult mutate(String op, Supplier<UUID> action) {
    try {
      UUID id = action.get();
      logger.info("{} succeeded for id {}", op, id);
      return QuerySuccess.newBuilder()
          .message(op + " succeeded")
          .id(id == null ? null : id.toString())
          .build();
    } catch (Exception e) {
      logger.error("{} failed", op, e);
      return StandardError.newBuilder()
          .message(op + " failed: " + e.getMessage())
          .build();
    }
  }
}
