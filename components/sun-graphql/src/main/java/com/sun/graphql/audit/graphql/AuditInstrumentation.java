package com.sun.graphql.audit.graphql;

import com.sun.base.audit.context.AuditContext;
import com.sun.base.audit.context.OperationMetadata;
import com.sun.base.audit.enums.AuditOutcome;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * GraphQL hook that records one namespaced operation per request. Sun's schema
 * nests operations under a per-service type, so the real operation sits at path
 * level 2 (e.g. {@code mutation { hadesMutations { createAnnotation } }}).
 */
@Component
public class AuditInstrumentation extends SimpleInstrumentation {

  private static final InstrumentationContext<Object> NOOP = new InstrumentationContext<>() {
    @Override
    public void onDispatched() {
      // no-op
    }

    @Override
    public void onCompleted(Object result, Throwable t) {
      // no-op
    }
  };

  private final AuditContext auditContext;
  private final AuditOperationRegistry registry;

  public AuditInstrumentation(AuditContext auditContext, AuditOperationRegistry registry) {
    this.auditContext = auditContext;
    this.registry = registry;
  }

  @Override
  public InstrumentationContext<Object> beginFieldFetch(
      InstrumentationFieldFetchParameters parameters, InstrumentationState state) {
    var env = parameters.getEnvironment();
    var stepInfo = env.getExecutionStepInfo();
    // Root selection is level 1; the namespaced operation is its child at level 2.
    if (stepInfo.getPath().getLevel() != 2) {
      return NOOP;
    }

    GraphQLType parentType = stepInfo.getParent().getType();
    GraphQLObjectType parentObject = (GraphQLObjectType) GraphQLTypeUtil.unwrapAll(parentType);
    String parentTypeName = parentObject.getName();
    String fieldName = stepInfo.getPath().getSegmentName();
    OperationMetadata metadata = registry.forParentType(parentTypeName, fieldName);
    Object variables = sanitizeForJson(env.getArguments());

    return new InstrumentationContext<>() {
      @Override
      public void onDispatched() {
        // no-op
      }

      @Override
      public void onCompleted(Object result, Throwable t) {
        AuditOutcome outcome;
        String errorMessage = null;
        if (t != null) {
          outcome = AuditOutcome.FAILURE;
          errorMessage = t.getMessage();
        } else {
          outcome = AuditOutcome.SUCCESS;
        }
        UUID targetEntityId = extractId(result);
        auditContext.addOperation(new AuditContext.AuditOperation(
            fieldName, metadata, variables, targetEntityId, outcome, errorMessage));
      }
    };
  }

  /**
   * Best-effort extraction of the acted-on entity id from the field result.
   *
   * @param result the field result
   * @return the id, or null if absent or not a UUID
   */
  private UUID extractId(Object result) {
    if (!(result instanceof Map<?, ?> map)) {
      return null;
    }
    Object id = map.get("id");
    if (id == null) {
      return null;
    }
    try {
      return UUID.fromString(id.toString());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Converts graphql argument values into JSON-serialisable forms so the audit
   * mapper can handle them.
   *
   * @param value the raw argument value
   * @return a value safe for Jackson serialisation
   */
  private Object sanitizeForJson(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Temporal || value instanceof Date) {
      return value.toString();
    }
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> out = new LinkedHashMap<>();
      for (Map.Entry<?, ?> e : map.entrySet()) {
        out.put(String.valueOf(e.getKey()), sanitizeForJson(e.getValue()));
      }
      return out;
    }
    if (value instanceof Iterable<?> iterable) {
      List<Object> out = new ArrayList<>();
      for (Object item : iterable) {
        out.add(sanitizeForJson(item));
      }
      return out;
    }
    return value;
  }
}
