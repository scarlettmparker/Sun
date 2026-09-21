package com.sun.graphql.config;

import com.sun.base.error.MutationException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.stereotype.Component;

/**
 * Maps MutationException to a GraphQL error. Every other exception returns null
 * so it falls through to the default DGS handler.
 */
@Component
public class MutationExceptionResolver extends DataFetcherExceptionResolverAdapter {

  @Override
  protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
    if (ex instanceof MutationException mutationException) {
      return GraphqlErrorBuilder.newError(env)
          .message(mutationException.getMessage())
          .extensions(Map.of("code", "MUTATION_FAILED"))
          .build();
    }
    return null;
  }
}
