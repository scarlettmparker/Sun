package com.sun.graphql.config;

import com.sun.base.error.MutationException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.persistence.EntityNotFoundException;
import java.util.Map;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.stereotype.Component;

/**
 * Maps the small set of exceptions raised by data fetchers to GraphQL errors
 * with a stable code. Everything else returns null so it falls through to the
 * default DGS handler.
 */
@Component
public class MutationExceptionResolver extends DataFetcherExceptionResolverAdapter {

  private static final Logger logger = LoggerFactory.getLogger(MutationExceptionResolver.class);

  @Override
  protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
    String field = env == null || env.getExecutionStepInfo() == null
        ? "unknown"
        : env.getExecutionStepInfo().getPath().toString();
    if (ex instanceof MutationException mutationException) {
      return error(env, mutationException.getMessage(), "MUTATION_FAILED");
    }
    if (ex instanceof IllegalArgumentException) {
      logger.warn("Bad request on {}: {}", field, ex.getMessage());
      return error(env, ex.getMessage(), "BAD_REQUEST");
    }
    if (ex instanceof EntityNotFoundException || ex instanceof NoSuchElementException) {
      return error(env, ex.getMessage(), "NOT_FOUND");
    }
    logger.error("Unhandled error on {}", field, ex);
    return null;
  }

  private static GraphQLError error(DataFetchingEnvironment env, String message, String code) {
    return GraphqlErrorBuilder.newError(env)
        .message(message)
        .extensions(Map.of("code", code))
        .build();
  }
}
