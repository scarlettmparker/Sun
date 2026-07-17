package com.sun.hades.graphql.mappers;

import com.sun.hades.codegen.types.ReaderObjectReference;
import com.sun.hades.service.RemoteObjectReference;
import org.springframework.stereotype.Component;

/**
 * Mapper for remote-object reference lookups.
 */
@Component
public class ReaderObjectReferenceMapper {

  /**
   * Maps a remote-object reference record to the GraphQL type.
   *
   * @param reference the reference record
   * @return the GraphQL ReaderObjectReference
   */
  public ReaderObjectReference map(RemoteObjectReference reference) {
    return ReaderObjectReference.newBuilder()
        .id(reference.id().toString())
        .ownerType(reference.ownerType())
        .ownerId(reference.ownerId().toString())
        .build();
  }
}
