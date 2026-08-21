package com.sun.echo.graphql.services;

import com.sun.echo.codegen.types.QueryResult;
import com.sun.echo.codegen.types.QuerySuccess;
import com.sun.echo.codegen.types.RemoteObjectReference;
import com.sun.echo.codegen.types.RemoteObjectType;
import com.sun.echo.codegen.types.StandardError;
import com.sun.echo.service.ChecklistDetailService;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL business logic for checklist detail remote-object links.
 */
@Service
public class ChecklistDetailGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(ChecklistDetailGraphQLService.class);

  private final ChecklistDetailService detailService;

  public ChecklistDetailGraphQLService(ChecklistDetailService detailService) {
    this.detailService = detailService;
  }

  /**
   * Finds every checklist detail that references any of the given remote-object
   * ids, tagged with the owning entity type.
   *
   * @param ids the foreign object ids to resolve
   * @return the GraphQL RemoteObjectReferences
   */
  @Transactional(readOnly = true)
  public List<RemoteObjectReference> locateRemoteObjects(List<String> ids) {
    return detailService.locateRemoteObjects(ids).stream()
        .map(ref -> RemoteObjectReference.newBuilder()
            .id(ref.id().toString())
            .ownerType(RemoteObjectType.valueOf(ref.ownerType()))
            .ownerId(ref.ownerId().toString())
            .description(ref.description())
            .build())
        .collect(Collectors.toList());
  }

  /**
   * Attaches a foreign object to an owner's detail.
   *
   * @param source the owning entity id
   * @param target the foreign object id to attach
   * @param ownerType optional owner type hint
   * @return a QueryResult
   */
  @Transactional
  public QueryResult attachObject(String source, String target, RemoteObjectType ownerType) {
    return mutate("attachObject", () -> detailService
        .attach(UUID.fromString(source), target, ownerType == null ? null : ownerType.name()));
  }

  /**
   * Removes a remote object reference from an owner's detail.
   */
  @Transactional
  public QueryResult detachObject(String source, String target, RemoteObjectType ownerType) {
    return mutate("detachObject", () -> detailService
        .detach(UUID.fromString(source), target, ownerType == null ? null : ownerType.name()));
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
