package com.sun.fates.graphql.mappers;

import com.sun.fates.codegen.types.Place;
import com.sun.fates.codegen.types.PlaceInput;
import com.sun.fates.model.PlaceEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting place entities to GraphQL types and back.
 */
@Component
public class PlaceMapper {

  /**
   * Maps a place entity to the GraphQL Place type.
   *
   * @param entity the place entity
   * @return the GraphQL Place
   */
  public Place map(PlaceEntity entity) {
    return Place.newBuilder()
        .id(entity.getId().toString())
        .line1(entity.getLine1())
        .line2(entity.getLine2())
        .city(entity.getCity())
        .region(entity.getRegion())
        .postalCode(entity.getPostalCode())
        .country(entity.getCountry())
        .build();
  }

  /**
   * Applies the input fields to the entity, for create or update.
   *
   * @param input the place input
   * @param entity the place entity to update
   */
  public void map(PlaceInput input, PlaceEntity entity) {
    if (input.getLine1() != null) {
      entity.setLine1(input.getLine1());
    }
    if (input.getLine2() != null) {
      entity.setLine2(input.getLine2());
    }
    if (input.getCity() != null) {
      entity.setCity(input.getCity());
    }
    if (input.getRegion() != null) {
      entity.setRegion(input.getRegion());
    }
    if (input.getPostalCode() != null) {
      entity.setPostalCode(input.getPostalCode());
    }
    if (input.getCountry() != null) {
      entity.setCountry(input.getCountry());
    }
  }
}
