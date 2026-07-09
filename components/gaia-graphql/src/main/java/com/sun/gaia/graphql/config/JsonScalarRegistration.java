package com.sun.gaia.graphql.config;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsRuntimeWiring;
import graphql.scalars.ExtendedScalars;
import graphql.schema.idl.RuntimeWiring;

/**
 * Registers the JSON scalar used for flexible property-set values.
 */
@DgsComponent
public class JsonScalarRegistration {

  /**
   * Adds the JSON scalar to the runtime wiring.
   *
   * @param builder the runtime wiring builder
   * @return the updated builder
   */
  @DgsRuntimeWiring
  public RuntimeWiring.Builder addJsonScalar(RuntimeWiring.Builder builder) {
    return builder.scalar(ExtendedScalars.Json);
  }
}
