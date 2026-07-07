package com.sun.narcissus.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.narcissus.codegen.types.ViewerQueries;
import com.sun.narcissus.codegen.types.VncCredentials;
import com.sun.narcissus.graphql.services.ViewerGraphQLService;
import org.springframework.beans.factory.annotation.Autowired;

@DgsComponent
public class ViewerDataFetcher {

  @Autowired
  private ViewerGraphQLService viewerGraphQLService;

  @DgsData(parentType = "Query", field = "viewerQueries")
  public ViewerQueries getViewerQueries() {
    return ViewerQueries.newBuilder().build();
  }

  @DgsData(parentType = "ViewerQueries", field = "vncCredentials")
  public VncCredentials vncCredentials() {
    return viewerGraphQLService.vncCredentials();
  }
}
