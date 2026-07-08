package com.sun.icarus.service;

import java.util.UUID;

/**
 * Reference to a thread that links a given remote object id.
 */
public record RemoteObjectReference(UUID id, String ownerType, UUID ownerId) {
}
