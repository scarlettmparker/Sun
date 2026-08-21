package com.sun.echo.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.echo.codegen.types.QueryResult;
import com.sun.echo.codegen.types.RemoteObjectReference;
import com.sun.echo.codegen.types.RemoteObjectType;
import com.sun.echo.graphql.services.ChecklistDetailGraphQLService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Data fetchers for checklist detail remote-object queries and mutations.
 */
@DgsComponent
public class ChecklistDetailDataFetcher {

  private final ChecklistDetailGraphQLService checklistDetailGraphQLService;

  public ChecklistDetailDataFetcher(ChecklistDetailGraphQLService checklistDetailGraphQLService) {
    this.checklistDetailGraphQLService = checklistDetailGraphQLService;
  }

  /**
   * Finds the checklist details that reference any of the given object ids.
   *
   * @param ids the object ids to resolve
   * @return a list of RemoteObjectReference objects
   */
  @DgsData(parentType = "ChecklistQueries", field = "locateRemoteObjects")
  @PreAuthorize("@permissions.has('graphql.echo.locateRemoteObjects')")
  public List<RemoteObjectReference> locateRemoteObjects(List<String> ids) {
    return checklistDetailGraphQLService.locateRemoteObjects(ids);
  }

  /**
   * Attaches a foreign object to a checklist detail.
   *
   * @param source the owning entity id
   * @param target the foreign object id to attach
   * @param ownerType an optional owner type hint (ENTRY, TEMPLATE, or ITEM)
   * @return a QueryResult
   */
  @DgsData(parentType = "ChecklistMutations", field = "attachObject")
  @PreAuthorize("@permissions.has('graphql.echo.attachObject')")
  public QueryResult attachObject(String source, String target, RemoteObjectType ownerType) {
    return checklistDetailGraphQLService.attachObject(source, target, ownerType);
  }

  @DgsData(parentType = "ChecklistMutations", field = "detachObject")
  @PreAuthorize("@permissions.has('graphql.echo.detachObject')")
  public QueryResult detachObject(String source, String target, RemoteObjectType ownerType) {
    return checklistDetailGraphQLService.detachObject(source, target, ownerType);
  }
}
