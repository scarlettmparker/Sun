package com.sun.graphql.audit.graphql;

import com.sun.base.audit.context.OperationMetadata;
import com.sun.base.audit.enums.OperationType;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Derives audit metadata for a GraphQL operation from its parent type. Sun's
 * schema namespaces every operation under a per-service type (HadesMutations,
 * GaiaQueries, ...), so the namespace and operation type come from that type's
 * name and the event type from the field name.
 */
@Component
public class AuditOperationRegistry {

  /**
   * Resolves audit metadata for a namespaced GraphQL field.
   *
   * @param parentTypeName the field's parent type, e.g. HadesMutations
   * @param fieldName the executed field, e.g. createAnnotation
   * @return metadata with derived namespace, operation type, and event type
   */
  public OperationMetadata forParentType(String parentTypeName, String fieldName) {
    OperationType operationType = operationTypeFor(parentTypeName);
    String namespace = namespaceFor(parentTypeName);
    String eventType = (namespace == null ? "" : namespace + "_")
        + (fieldName == null ? "UNKNOWN" : fieldName.toUpperCase());
    return new OperationMetadata(eventType, operationType, namespace, null, Set.of());
  }

  /**
   * Resolves audit metadata for a REST endpoint. Sun is GraphQL-first, so all
   * non-GraphQL requests fall back to an anonymous REST entry.
   *
   * @param method the HTTP method, e.g. POST
   * @param path the request path
   * @return anonymous REST metadata
   */
  public OperationMetadata forEndpoint(String method, String path) {
    return OperationMetadata.unknown(OperationType.REST, null);
  }

  private OperationType operationTypeFor(String parentTypeName) {
    if (parentTypeName == null) {
      return OperationType.QUERY;
    }
    if (parentTypeName.endsWith("Mutations") || parentTypeName.equals("Mutation")) {
      return OperationType.MUTATION;
    }
    return OperationType.QUERY;
  }

  private String namespaceFor(String parentTypeName) {
    if (parentTypeName == null) {
      return null;
    }
    for (String suffix : new String[] {"Mutations", "Queries", "Mutation", "Query"}) {
      if (parentTypeName.endsWith(suffix) && parentTypeName.length() > suffix.length()) {
        return parentTypeName.substring(0, parentTypeName.length() - suffix.length()).toUpperCase();
      }
    }
    // Bare Query/Mutation root - no namespace.
    return null;
  }
}
