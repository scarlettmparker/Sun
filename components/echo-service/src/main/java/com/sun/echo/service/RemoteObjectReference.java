package com.sun.echo.service;

import java.util.UUID;

/**
 * A reference to a checklist detail that owns one or more remote-object links.
 * Returned by locateRemoteObjects so callers can resolve which checklist entity
 * references a given foreign object. The ownerType is ENTRY, TEMPLATE, or ITEM.
 */
public record RemoteObjectReference(UUID id, String ownerType, UUID ownerId, String description) {
}
