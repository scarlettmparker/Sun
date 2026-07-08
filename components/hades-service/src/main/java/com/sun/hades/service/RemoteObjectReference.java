package com.sun.hades.service;

import java.util.UUID;

/**
 * Reference to an annotation that links a given remote object id.
 */
public record RemoteObjectReference(UUID id, String ownerType, UUID ownerId, String description) {
}
