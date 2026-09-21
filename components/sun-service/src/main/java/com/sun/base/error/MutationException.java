package com.sun.base.error;

/**
 * Raised when a GraphQL mutation cannot complete. The resolver in sun-graphql
 * maps it to a GraphQL error carrying this message.
 */
public class MutationException extends RuntimeException {

  public MutationException(String message) {
    super(message);
  }

  public MutationException(String message, Throwable cause) {
    super(message, cause);
  }
}
