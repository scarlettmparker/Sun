package com.sun.graphql.audit.graphql;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.base.audit.annotation.Audited;
import com.sun.base.audit.annotation.NotAudited;
import com.sun.base.audit.enums.OperationType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Collects audit metadata from {@link Audited} on data fetcher and REST
 * controller methods, and fails startup when an operation carries neither
 * {@link Audited} nor {@link NotAudited}.
 */
@Component
public class AuditedOperationRegistry implements SmartInitializingSingleton {

  private static final Logger logger = LoggerFactory.getLogger(AuditedOperationRegistry.class);

  private final ApplicationContext context;
  private final AuditOperationRegistry registry;

  public AuditedOperationRegistry(ApplicationContext context, AuditOperationRegistry registry) {
    this.context = context;
    this.registry = registry;
  }

  @Override
  public void afterSingletonsInstantiated() {
    Discovered discovered = discover();
    registry.register(discovered.operations());
    registry.registerEndpoints(discovered.endpoints());
  }

  /**
   * Scans every data fetcher and controller for audit annotations.
   *
   * @return the discovered operations and endpoints
   */
  private Discovered discover() {
    Map<String, AuditedMetadata> operations = new HashMap<>();
    Map<String, AuditedMetadata> endpoints = new HashMap<>();
    Set<String> uncoveredOperations = new HashSet<>();
    Set<String> uncoveredEndpoints = new HashSet<>();

    for (Object fetcher : context.getBeansWithAnnotation(DgsComponent.class).values()) {
      Class<?> target = AopUtils.getTargetClass(fetcher);
      for (Method method : ReflectionUtils.getAllDeclaredMethods(target)) {
        DgsData data = method.getAnnotation(DgsData.class);
        if (data == null) {
          continue;
        }
        Audited audited = method.getAnnotation(Audited.class);
        NotAudited notAudited = method.getAnnotation(NotAudited.class);
        String key = data.parentType() + "." + data.field();
        if (audited == null && notAudited == null) {
          uncoveredOperations.add(key);
          continue;
        }
        operations.put(key, auditedMetadata(method, audited, notAudited));
      }
    }

    for (Object controller : context.getBeansWithAnnotation(Controller.class).values()) {
      Class<?> target = AopUtils.getTargetClass(controller);
      if (!isAppController(target)) {
        continue;
      }
      for (Method method : ReflectionUtils.getAllDeclaredMethods(target)) {
        List<String> keys = endpointKeys(target, method);
        if (keys.isEmpty()) {
          continue;
        }
        Audited audited = method.getAnnotation(Audited.class);
        NotAudited notAudited = method.getAnnotation(NotAudited.class);
        if (audited == null && notAudited == null) {
          uncoveredEndpoints.addAll(keys);
          continue;
        }
        AuditedMetadata metadata = auditedMetadata(method, audited, notAudited);
        keys.forEach(key -> endpoints.put(key, metadata));
      }
    }

    if (!uncoveredOperations.isEmpty()) {
      throw new IllegalStateException(
          "Data fetchers missing @Audited or @NotAudited: " + String.join(", ", uncoveredOperations));
    }
    if (!uncoveredEndpoints.isEmpty()) {
      throw new IllegalStateException(
          "REST endpoints missing @Audited or @NotAudited: " + String.join(", ", uncoveredEndpoints));
    }
    return new Discovered(operations, endpoints);
  }

  /**
   * Builds metadata for one annotated method, logging the reason when excluded.
   *
   * @param method the annotated method
   * @param audited the audited annotation, or null
   * @param notAudited the not-audited annotation, or null
   * @return the metadata
   */
  private AuditedMetadata auditedMetadata(Method method, Audited audited, NotAudited notAudited) {
    if (notAudited != null) {
      logger.debug("Audit excluded for {}: {}", method.getName(), notAudited.reason());
      return AuditedMetadata.notAudited();
    }
    return AuditedMetadata.of(audited.event(), audited.entity(),
        Set.of(audited.sensitiveFields()), Set.of());
  }

  /**
   * Reports whether a controller class belongs to this application.
   *
   * @param target the controller class
   * @return true when the class is under the application package
   */
  private static boolean isAppController(Class<?> target) {
    return target.getPackageName().startsWith("com.sun.");
  }

  /**
   * Resolves the audit keys for a Spring MVC handler method.
   *
   * @param controller the controller class holding the method
   * @param method the handler method
   * @return keys shaped "METHOD /path", or empty when the method is not mapped
   */
  private static List<String> endpointKeys(Class<?> controller, Method method) {
    PathsAndMethods mapping = mappingOf(method);
    if (mapping == null) {
      return List.of();
    }
    List<String> prefixes = requestPaths(
        AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class));
    if (prefixes.isEmpty()) {
      prefixes = List.of("");
    }
    List<String> keys = new ArrayList<>();
    for (String prefix : prefixes) {
      for (String path : mapping.paths()) {
        String fullPath = normalize(prefix, path);
        if (mapping.methods().isEmpty()) {
          keys.add("* " + fullPath);
        } else {
          mapping.methods().forEach(rm -> keys.add(rm.name() + " " + fullPath));
        }
      }
    }
    return keys;
  }

  /**
   * Reads the mapping annotation from a handler method.
   *
   * @param method the handler method
   * @return the paths and HTTP methods, or null when the method is not mapped
   */
  private static PathsAndMethods mappingOf(Method method) {
    GetMapping get = AnnotatedElementUtils.findMergedAnnotation(method, GetMapping.class);
    if (get != null) {
      return new PathsAndMethods(requestPaths(get.value(), get.path()), Set.of(RequestMethod.GET));
    }
    PostMapping post = AnnotatedElementUtils.findMergedAnnotation(method, PostMapping.class);
    if (post != null) {
      return new PathsAndMethods(requestPaths(post.value(), post.path()), Set.of(RequestMethod.POST));
    }
    PutMapping put = AnnotatedElementUtils.findMergedAnnotation(method, PutMapping.class);
    if (put != null) {
      return new PathsAndMethods(requestPaths(put.value(), put.path()), Set.of(RequestMethod.PUT));
    }
    DeleteMapping delete = AnnotatedElementUtils.findMergedAnnotation(method, DeleteMapping.class);
    if (delete != null) {
      return new PathsAndMethods(requestPaths(delete.value(), delete.path()), Set.of(RequestMethod.DELETE));
    }
    PatchMapping patch = AnnotatedElementUtils.findMergedAnnotation(method, PatchMapping.class);
    if (patch != null) {
      return new PathsAndMethods(requestPaths(patch.value(), patch.path()), Set.of(RequestMethod.PATCH));
    }
    RequestMapping request = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
    if (request != null) {
      return new PathsAndMethods(requestPaths(request.value(), request.path()),
          Set.copyOf(Arrays.asList(request.method())));
    }
    return null;
  }

  /**
   * Reads the path patterns from a request mapping annotation.
   *
   * @param mapping the mapping annotation, or null
   * @return the declared paths, empty when none are set
   */
  private static List<String> requestPaths(RequestMapping mapping) {
    if (mapping == null) {
      return List.of();
    }
    return requestPaths(mapping.value(), mapping.path());
  }

  /**
   * Combines the value and path aliases into one list.
   *
   * @param value the value alias
   * @param path the path alias
   * @return the declared paths
   */
  private static List<String> requestPaths(String[] value, String[] path) {
    List<String> paths = new ArrayList<>();
    paths.addAll(Arrays.asList(value));
    paths.addAll(Arrays.asList(path));
    return paths;
  }

  /**
   * Joins a controller prefix and a method path into one normalised path.
   *
   * @param prefix the class-level request mapping prefix
   * @param path the method-level path
   * @return the combined path with a single leading slash and no trailing slash
   */
  private static String normalize(String prefix, String path) {
    String combined = "/" + stripSlashes(prefix) + "/" + stripSlashes(path);
    combined = combined.replaceAll("/{2,}", "/");
    if (combined.length() > 1 && combined.endsWith("/")) {
      combined = combined.substring(0, combined.length() - 1);
    }
    return combined;
  }

  /**
   * Removes leading and trailing slashes from a path segment.
   *
   * @param value the path segment
   * @return the segment without surrounding slashes
   */
  private static String stripSlashes(String value) {
    String trimmed = value == null ? "" : value.trim();
    while (trimmed.startsWith("/")) {
      trimmed = trimmed.substring(1);
    }
    while (trimmed.endsWith("/")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    return trimmed;
  }

  /**
   * Discovered audit metadata split by target.
   *
   * @param operations metadata keyed by "ParentType.field"
   * @param endpoints metadata keyed by "METHOD /path"
   */
  private record Discovered(Map<String, AuditedMetadata> operations,
                            Map<String, AuditedMetadata> endpoints) {}

  /**
   * The paths and HTTP methods declared by an MVC mapping annotation.
   *
   * @param paths the declared path patterns
   * @param methods the declared HTTP methods, empty meaning any method
   */
  private record PathsAndMethods(List<String> paths, Set<RequestMethod> methods) {}

  /**
   * Audit metadata discovered from a data fetcher or controller method.
   *
   * @param excluded whether the operation is left out of the trail
   * @param event the event label
   * @param entity the entity acted on
   * @param sensitiveFields fields masked in the payload
   * @param encryptFields fields stripped and stored encrypted
   */
  public record AuditedMetadata(
      boolean excluded,
      String event,
      String entity,
      Set<String> sensitiveFields,
      Set<String> encryptFields) {

    /**
     * Builds metadata for an audited operation.
     *
     * @param event the event label
     * @param entity the entity acted on
     * @param sensitiveFields fields masked in the payload
     * @param encryptFields fields stripped and stored encrypted
     * @return the metadata
     */
    public static AuditedMetadata of(String event, String entity, Set<String> sensitiveFields,
                                     Set<String> encryptFields) {
      return new AuditedMetadata(false, event, entity, sensitiveFields, encryptFields);
    }

    /**
     * Builds metadata for an operation left out of the trail.
     *
     * @return the metadata
     */
    public static AuditedMetadata notAudited() {
      return new AuditedMetadata(true, null, null, Set.of(), Set.of());
    }
  }
}
