package com.sun.gaia.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.gaia.codegen.types.GaiaMutations;
import com.sun.gaia.codegen.types.GaiaQueries;

/**
 * Root entry points for Gaia queries and mutations.
 */
@DgsComponent
public class GaiaRootDataFetcher {

  private final GaiaAuthHelper authHelper;

  public GaiaRootDataFetcher(GaiaAuthHelper authHelper) {
    this.authHelper = authHelper;
  }

  /**
   * Provides the access queries object.
   *
   * @return the GaiaQueries builder
   */
  @DgsData(parentType = "Query", field = "gaiaQueries")
  public GaiaQueries getGaiaQueries() {
    authHelper.resolveUserFromRequest();
    return GaiaQueries.newBuilder().build();
  }

  /**
   * Provides the access mutations object.
   *
   * @return the GaiaMutations builder
   */
  @DgsData(parentType = "Mutation", field = "gaiaMutations")
  public GaiaMutations getGaiaMutations() {
    authHelper.resolveUserFromRequest();
    return GaiaMutations.newBuilder().build();
  }
}
