package com.sun.echo.graphql.services;

import com.sun.base.error.MutationException;
import com.sun.echo.codegen.types.AttachObjectResponse;
import com.sun.echo.codegen.types.DetachObjectResponse;
import com.sun.echo.codegen.types.RemoteObjectReference;
import com.sun.echo.codegen.types.RemoteObjectType;
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
   * @return the updated detail id
   */
  @Transactional
  public AttachObjectResponse attachObject(String source, String target, RemoteObjectType ownerType) {
    return mutate("attachObject", () -> {
      UUID id = detailService.attach(UUID.fromString(source), target,
          ownerType == null ? null : ownerType.name());
      return AttachObjectResponse.newBuilder()
          .message("Object attached successfully")
          .id(id.toString())
          .build();
    });
  }

  /**
   * Removes a remote object reference from an owner's detail.
   *
   * @param source the owning entity id
   * @param target the foreign object id to detach
   * @param ownerType optional owner type hint
   * @return the updated detail id
   */
  @Transactional
  public DetachObjectResponse detachObject(String source, String target, RemoteObjectType ownerType) {
    return mutate("detachObject", () -> {
      UUID id = detailService.detach(UUID.fromString(source), target,
          ownerType == null ? null : ownerType.name());
      return DetachObjectResponse.newBuilder()
          .message("Object detached successfully")
          .id(id.toString())
          .build();
    });
  }

  /**
   * Runs a mutation, logging the operation and raising a MutationException on failure.
   *
   * @param op the operation name, for logging and messages
   * @param action the mutation, returning its response
   * @return the mutation response
   */
  private <T> T mutate(String op, Supplier<T> action) {
    try {
      T response = action.get();
      logger.info("{} succeeded", op);
      return response;
    } catch (Exception e) {
      logger.error("{} failed", op, e);
      throw new MutationException(op + " failed: " + e.getMessage(), e);
    }
  }
}
