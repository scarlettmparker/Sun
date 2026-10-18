package com.sun.graphql.audit.graphql;

import com.sun.base.audit.context.OperationMetadata;
import com.sun.base.audit.enums.OperationType;
import com.sun.graphql.audit.graphql.AuditedOperationRegistry.AuditedMetadata;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Holds the audit metadata discovered from data fetcher and controller
 * annotations, and resolves it for a GraphQL field or REST endpoint.
 */
@Component
public class AuditOperationRegistry {

  private final Map<String, AuditedMetadata> byOperation = new HashMap<>();

  /**
   * Keyed by "METHOD /path" for REST endpoints, which have no GraphQL field.
   */
  private final Map<String, AuditedMetadata> byEndpoint = new HashMap<>();

  /**
   * Replaces the discovered GraphQL metadata.
   *
   * @param discovered metadata keyed by "ParentType.field"
   */
  public void register(Map<String, AuditedMetadata> discovered) {
    byOperation.clear();
    byOperation.putAll(discovered);
  }

  /**
   * Replaces the discovered REST endpoint metadata.
   *
   * @param discovered metadata keyed by "METHOD /path"
   */
  public void registerEndpoints(Map<String, AuditedMetadata> discovered) {
    byEndpoint.clear();
    byEndpoint.putAll(discovered);
  }

  /**
   * Resolves audit metadata for a GraphQL field.
   *
   * @param parentType the field's parent type, e.g. HadesMutations
   * @param field the executed field, e.g. createAnnotation
   * @return the metadata, or an unknown default when the field has no entry
   */
  public OperationMetadata forOperation(String parentType, String field) {
    OperationType operationType = operationTypeFor(parentType);
    AuditedMetadata meta = field == null ? null : byOperation.get(parentType + "." + field);
    if (meta == null || meta.excluded()) {
      return OperationMetadata.unknown(operationType, null);
    }
    return toMetadata(meta, parentType);
  }

  /**
   * Resolves audit metadata for a REST endpoint.
   *
   * @param method the HTTP method, e.g. POST
   * @param path the request path, e.g. /api/headscale/nodes
   * @return the configured metadata, or an anonymous default
   */
  public OperationMetadata forEndpoint(String method, String path) {
    AuditedMetadata meta = endpointMeta(method, path);
    if (meta == null || meta.excluded()) {
      return OperationMetadata.unknown(OperationType.REST, null);
    }
    return new OperationMetadata(meta.event(), OperationType.REST, null, meta.entity(),
        meta.sensitiveFields(), meta.encryptFields());
  }

  /**
   * Reports whether a REST endpoint is configured for auditing.
   *
   * @param method the HTTP method, e.g. POST
   * @param path the request path
   * @return true when the endpoint has an audited entry
   */
  public boolean isAuditedEndpoint(String method, String path) {
    AuditedMetadata meta = endpointMeta(method, path);
    return meta != null && !meta.excluded();
  }

  /**
   * Reports whether a GraphQL field is left out of the trail.
   *
   * @param parentType the field's parent type
   * @param field the executed field
   * @return true when the field carries a not-audited entry
   */
  public boolean isExcluded(String parentType, String field) {
    AuditedMetadata meta = field == null ? null : byOperation.get(parentType + "." + field);
    return meta != null && meta.excluded();
  }

  /**
   * Looks up an endpoint by exact method, falling back to a wildcard entry
   * declared without a specific HTTP method.
   *
   * @param method the HTTP method
   * @param path the request path
   * @return the metadata, or null when unmapped
   */
  private AuditedMetadata endpointMeta(String method, String path) {
    if (method == null || path == null) {
      return null;
    }
    AuditedMetadata meta = byEndpoint.get(method.toUpperCase() + " " + path);
    return meta != null ? meta : byEndpoint.get("* " + path);
  }

  /**
   * Converts discovered metadata into the shape the audit writer consumes.
   *
   * @param meta the discovered metadata
   * @param parentType the field's parent type
   * @return the operation metadata
   */
  private static OperationMetadata toMetadata(AuditedMetadata meta, String parentType) {
    return new OperationMetadata(meta.event(), operationTypeFor(parentType), namespaceFor(parentType),
        meta.entity(), meta.sensitiveFields(), meta.encryptFields());
  }

  /**
   * Resolves which root type a parent type belongs to.
   *
   * @param parentType the field's parent type
   * @return MUTATION when the parent is a mutation type, otherwise QUERY
   */
  private static OperationType operationTypeFor(String parentType) {
    if (parentType == null) {
      return OperationType.QUERY;
    }
    if (parentType.endsWith("Mutations") || parentType.equals("Mutation")) {
      return OperationType.MUTATION;
    }
    return OperationType.QUERY;
  }

  /**
   * Derives the namespace from a parent type name.
   *
   * @param parentType the field's parent type, e.g. HadesQueries
   * @return the namespace, e.g. HADES, or null for a bare root type
   */
  private static String namespaceFor(String parentType) {
    if (parentType == null) {
      return null;
    }
    for (String suffix : new String[] {"Mutations", "Queries", "Mutation", "Query"}) {
      if (parentType.endsWith(suffix) && parentType.length() > suffix.length()) {
        return parentType.substring(0, parentType.length() - suffix.length()).toUpperCase();
      }
    }
    return null;
  }
}
