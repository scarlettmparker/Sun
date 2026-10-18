package com.sun.base.audit.snapshot;

import com.sun.base.audit.annotation.Audited;
import com.sun.base.audit.context.AuditContext;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

/**
 * Captures the prior state of an entity before an audited edit runs, on the
 * request thread where the old row is still readable.
 */
@Aspect
@Component
public class AuditSnapshotAspect {

  private final AuditContext auditContext;
  private final AuditSnapshotLoaders loaders;

  public AuditSnapshotAspect(AuditContext auditContext, AuditSnapshotLoaders loaders) {
    this.auditContext = auditContext;
    this.loaders = loaders;
  }

  /**
   * Snapshots the target entity's prior state before the edit proceeds.
   *
   * @param joinPoint the intercepted audited method
   * @return the method's own result
   * @throws Throwable as thrown by the intercepted method
   */
  @Around("@annotation(Audited)")
  public Object captureBeforeState(ProceedingJoinPoint joinPoint) throws Throwable {
    try {
      Audited audited = resolveAnnotation(joinPoint);
      if (audited != null && audited.beforeFields().length > 0) {
        capture(joinPoint, audited);
      }
    } catch (Exception e) {
      // A snapshot failure must not block the edit.
    }
    return joinPoint.proceed();
  }

  /**
   * Resolves the audited annotation from the method or its target class.
   *
   * @param joinPoint the intercepted method
   * @return the annotation, or null when absent
   */
  private Audited resolveAnnotation(ProceedingJoinPoint joinPoint) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = AopUtils.getMostSpecificMethod(signature.getMethod(), joinPoint.getTarget().getClass());
    return AnnotationUtils.findAnnotation(method, Audited.class);
  }

  /**
   * Loads the prior state and stashes the requested fields in the audit context.
   *
   * @param joinPoint the intercepted method
   * @param audited the resolved annotation
   */
  private void capture(ProceedingJoinPoint joinPoint, Audited audited) {
    UUID id = extractId(joinPoint);
    if (id == null) {
      return;
    }
    Optional<Map<String, Object>> prior = loaders.load(audited.entity(), id);
    prior.ifPresent(state -> auditContext.setPendingBeforePayload(project(state, audited.beforeFields())));
  }

  /**
   * Extracts the target entity id from the arguments, supporting a single input
   * record with an id or userId accessor or a scalar id parameter.
   *
   * @param joinPoint the intercepted method
   * @return the entity id, or null when none can be resolved
   */
  private UUID extractId(ProceedingJoinPoint joinPoint) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    String[] names = signature.getParameterNames();
    Object[] args = joinPoint.getArgs();

    if (names != null) {
      for (int i = 0; i < names.length; i++) {
        if ("id".equals(names[i])) {
          UUID id = asUuid(args[i]);
          if (id != null) {
            return id;
          }
        }
      }
    }

    for (Object arg : args) {
      UUID id = readAccessor(arg, "id");
      if (id == null) {
        id = readAccessor(arg, "userId");
      }
      if (id != null) {
        return id;
      }
    }
    return null;
  }

  /**
   * Reads a UUID from a no-arg accessor on an input record.
   *
   * @param target the input record, or any object
   * @param accessor the accessor name, e.g. id
   * @return the id, or null when absent or not a UUID
   */
  private UUID readAccessor(Object target, String accessor) {
    if (target == null) {
      return null;
    }
    try {
      Method method = target.getClass().getMethod(accessor);
      return asUuid(method.invoke(target));
    } catch (ReflectiveOperationException e) {
      return null;
    }
  }

  /**
   * Coerces a value to a UUID.
   *
   * @param value the value, or null
   * @return the UUID, or null when it is not one
   */
  private UUID asUuid(Object value) {
    if (value instanceof UUID uuid) {
      return uuid;
    }
    if (value instanceof String text && !text.isBlank()) {
      try {
        return UUID.fromString(text);
      } catch (IllegalArgumentException e) {
        return null;
      }
    }
    return null;
  }

  /**
   * Keeps only the requested fields from the loaded prior state.
   *
   * @param state the full prior-state map
   * @param fields the fields to keep
   * @return the projected map, in the requested order
   */
  private Map<String, Object> project(Map<String, Object> state, String[] fields) {
    Map<String, Object> projected = new LinkedHashMap<>();
    for (String field : fields) {
      projected.put(field, state.get(field));
    }
    return projected;
  }
}
