package com.sun.graphql.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import graphql.ExecutionInput;
import graphql.GraphqlErrorBuilder;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.Node;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * Parses each document once, caches it, and rejects documents that exceed the
 * depth or field budget.
 */
@Component
public class PreparsedQueryProvider implements PreparsedDocumentProvider {

  private static final int MAX_DEPTH = 12;
  private static final int MAX_COMPLEXITY = 300;

  private final Cache<String, PreparsedDocumentEntry> cache = Caffeine.newBuilder()
      .expireAfterAccess(Duration.ofMinutes(10))
      .maximumSize(1000)
      .build();

  @Override
  public CompletableFuture<PreparsedDocumentEntry> getDocumentAsync(
      ExecutionInput executionInput,
      Function<ExecutionInput, PreparsedDocumentEntry> parseAndValidateFunction) {
    String query = executionInput.getQuery();
    PreparsedDocumentEntry cached = cache.getIfPresent(query);
    if (cached != null) {
      return CompletableFuture.completedFuture(cached);
    }

    PreparsedDocumentEntry entry = parseAndValidateFunction.apply(executionInput);
    if (entry.hasErrors() || entry.getDocument() == null) {
      return CompletableFuture.completedFuture(entry);
    }

    Document document = entry.getDocument();
    int depth = maxDepth(document);
    if (depth > MAX_DEPTH) {
      return CompletableFuture.completedFuture(
          reject("Query depth " + depth + " exceeds limit " + MAX_DEPTH));
    }
    int complexity = countFields(document);
    if (complexity > MAX_COMPLEXITY) {
      return CompletableFuture.completedFuture(
          reject("Query complexity " + complexity + " exceeds limit " + MAX_COMPLEXITY));
    }

    cache.put(query, entry);
    return CompletableFuture.completedFuture(entry);
  }

  /**
   * Builds a parse entry carrying a single validation error.
   *
   * @param message the rejection message
   * @return the rejected entry
   */
  private static PreparsedDocumentEntry reject(String message) {
    return new PreparsedDocumentEntry(GraphqlErrorBuilder.newError().message(message).build());
  }

  private static int maxDepth(Document document) {
    int max = 0;
    for (Node child : document.getChildren()) {
      if (child instanceof OperationDefinition op) {
        max = Math.max(max, depthOf(op.getSelectionSet(), 1));
      }
    }
    return max;
  }

  private static int depthOf(SelectionSet set, int current) {
    if (set == null || current > MAX_DEPTH) {
      return current;
    }
    int max = current;
    for (Selection<?> selection : set.getSelections()) {
      if (selection instanceof Field field && field.getSelectionSet() != null) {
        max = Math.max(max, depthOf(field.getSelectionSet(), current + 1));
      } else if (selection instanceof InlineFragment frag && frag.getSelectionSet() != null) {
        max = Math.max(max, depthOf(frag.getSelectionSet(), current + 1));
      }
    }
    return max;
  }

  private static int countFields(Document document) {
    int count = 0;
    for (Node child : document.getChildren()) {
      if (child instanceof OperationDefinition op) {
        count += countFields(op.getSelectionSet());
      }
    }
    return count;
  }

  private static int countFields(SelectionSet set) {
    if (set == null) {
      return 0;
    }
    int count = 0;
    for (Selection<?> selection : set.getSelections()) {
      if (selection instanceof Field) {
        count += 1;
      } else if (selection instanceof InlineFragment frag) {
        count += countFields(frag.getSelectionSet());
      } else if (selection instanceof FragmentSpread) {
        count += 1;
      }
    }
    return count;
  }
}
