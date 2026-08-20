package com.sun.graphql.config;

import graphql.ExecutionInput;
import graphql.GraphQLException;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationCreateStateParameters;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.Node;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.parser.InvalidSyntaxException;
import graphql.parser.Parser;
import org.springframework.stereotype.Component;

/**
 * Rejects queries whose depth or field-count exceeds a fixed budget, so a single
 * pathological document can't exhaust the connection pool.
 */
@Component
public class QueryGuardInstrumentation extends SimpleInstrumentation {

  private static final int MAX_DEPTH = 12;
  private static final int MAX_COMPLEXITY = 300;

  private static final InstrumentationContext<Document> NOOP =
      new InstrumentationContext<>() {
        @Override
        public void onDispatched() {
        }

        @Override
        public void onCompleted(Document result, Throwable t) {
        }
      };

  @Override
  public InstrumentationContext<Document> beginParse(
      InstrumentationExecutionParameters parameters, InstrumentationState state) {
    try {
      ExecutionInput input = parameters.getExecutionInput();
      Document document = new Parser().parseDocument(input.getQuery());
      int depth = maxDepth(document);
      int complexity = countFields(document);
      if (depth > MAX_DEPTH) {
        throw new GraphQLException("Query depth " + depth + " exceeds limit " + MAX_DEPTH);
      }
      if (complexity > MAX_COMPLEXITY) {
        throw new GraphQLException(
            "Query complexity " + complexity + " exceeds limit " + MAX_COMPLEXITY);
      }
    } catch (InvalidSyntaxException e) {
      return NOOP;
    }
    return NOOP;
  }

  private int maxDepth(Document document) {
    int max = 0;
    for (Node child : document.getChildren()) {
      if (child instanceof OperationDefinition op) {
        max = Math.max(max, depthOf(op.getSelectionSet(), 1));
      }
    }
    return max;
  }

  private int depthOf(SelectionSet set, int current) {
    if (set == null) {
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

  private int countFields(Document document) {
    int count = 0;
    for (Node child : document.getChildren()) {
      if (child instanceof OperationDefinition op) {
        count += countFields(op.getSelectionSet());
      }
    }
    return count;
  }

  private int countFields(SelectionSet set) {
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
