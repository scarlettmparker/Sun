package com.sun.hades.service;

import com.sun.hades.model.enums.CefrLevel;
import com.sun.hades.model.enums.ReaderTextStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read projection for reader text lists.
 */
public record ReaderTextSummary(
    UUID id,
    String title,
    String language,
    CefrLevel level,
    UUID ownerId,
    UUID sourceId,
    ReaderTextStatus status,
    LocalDateTime createdAt,
    LocalDateTime lastUpdatedAt) {
}
