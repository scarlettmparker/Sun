package com.sun.base.audit.context;

import com.sun.base.audit.context.AuditContext.AuditOperation;
import java.util.List;
import java.util.UUID;

/**
 * Immutable copy of one request's audit data, safe to pass to the async writer.
 */
public record AuditRequestSnapshot(
    /**
     * The operations captured for the request.
     */
    List<AuditOperation> operations,
    /**
     * The request correlation id.
     */
    UUID correlationId,
    /**
     * The calling user, or null when anonymous.
     */
    UUID userId,
    /**
     * The request URI.
     */
    String endpoint,
    /**
     * The best-guess client IP.
     */
    String ipAddress,
    /**
     * The caller's user-agent.
     */
    String userAgent,
    /**
     * The response status code.
     */
    int httpStatus) {}
