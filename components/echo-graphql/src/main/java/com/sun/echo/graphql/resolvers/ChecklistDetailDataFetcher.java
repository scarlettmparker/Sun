package com.sun.echo.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.base.error.MutationException;
import com.sun.echo.codegen.types.AttachChecklistObjectResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data fetchers for checklist detail remote-object queries and mutations.
 */
@DgsComponent
public class ChecklistDetailDataFetcher {

  private static final Logger logger = LoggerFactory.getLogger(ChecklistDetailDataFetcher.class);

  private final ChecklistDetailService detailService;

  public ChecklistDetailDataFetcher(ChecklistDetailService detailService) {
    this.detailService = detailService;
  }

  /**
   * Finds the checklist details that reference any of the given object ids.
   *
   * @param ids the object ids to resolve
   * @return a list of RemoteObjectReference objects
   */
  @DgsData(parentType = "ChecklistQueries", field = "locateRemoteObjects")
  @PreAuthorize("@permissions.has('graphql.echo.locateRemoteObjects')")
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
   * Attaches a foreign object to a checklist detail.
   *
   * @param source the owning entity id
   * @param target the foreign object id to attach
   * @param ownerType an optional owner type hint (ENTRY, TEMPLATE, or ITEM)
   * @return the updated detail id
   */
  @DgsData(parentType = "ChecklistMutations", field = "attachObject")
  @PreAuthorize("@permissions.has('graphql.echo.attachObject')")
  @Transactional
  public AttachChecklistObjectResponse attachObject(String source, String target, RemoteObjectType ownerType) {
    return mutate("attachObject", () -> {
      UUID id = detailService.attach(UUID.fromString(source), target,
          ownerType == null ? null : ownerType.name());
      return AttachChecklistObjectResponse.newBuilder()
          .message("Object attached successfully")
          .id(id.toString())
          .build();
    });
  }

  /**
   * Detaches a foreign object from a checklist detail.
   *
   * @param source the owning entity id
   * @param target the foreign object id to detach
   * @param ownerType an optional owner type hint (ENTRY, TEMPLATE, or ITEM)
   * @return the updated detail id
   */
  @DgsData(parentType = "ChecklistMutations", field = "detachObject")
  @PreAuthorize("@permissions.has('graphql.echo.detachObject')")
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
