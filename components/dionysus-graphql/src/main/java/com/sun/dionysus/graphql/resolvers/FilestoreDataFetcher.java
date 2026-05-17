package com.sun.dionysus.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.dionysus.graphql.services.FilestoreGraphQLService;
import com.sun.dionysus.codegen.types.FilestoreQueries;
import org.springframework.beans.factory.annotation.Autowired;

@DgsComponent
public class FilestoreDataFetcher {

    @Autowired
    private FilestoreGraphQLService filestoreGraphQLService;

    @DgsData(parentType = "Query", field = "filestoreQueries")
    public FilestoreQueries getFilestoreQueries() {
        return FilestoreQueries.newBuilder().build();
    }

    @DgsData(parentType = "FilestoreQueries", field = "health")
    public String health() {
        return filestoreGraphQLService.health();
    }
}
