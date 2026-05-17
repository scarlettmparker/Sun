package com.sun.dionysus.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.dionysus.graphql.services.DionysusGraphQLService;
import com.sun.dionysus.codegen.types.DionysusQueries;
import org.springframework.beans.factory.annotation.Autowired;

@DgsComponent
public class DionysusDataFetcher {

    @Autowired
    private DionysusGraphQLService dionysusGraphQLService;

    @DgsData(parentType = "Query", field = "dionysusQueries")
    public DionysusQueries getDionysusQueries() {
        return DionysusQueries.newBuilder().build();
    }

    @DgsData(parentType = "DionysusQueries", field = "health")
    public String health() {
        return dionysusGraphQLService.health();
    }
}
