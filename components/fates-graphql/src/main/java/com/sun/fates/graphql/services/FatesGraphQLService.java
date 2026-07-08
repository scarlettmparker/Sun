package com.sun.fates.graphql.services;

import com.sun.fates.codegen.types.Person;
import com.sun.fates.codegen.types.PersonInput;
import com.sun.fates.codegen.types.Place;
import com.sun.fates.codegen.types.PlaceInput;
import com.sun.fates.codegen.types.QueryResult;
import com.sun.fates.codegen.types.QuerySuccess;
import com.sun.fates.codegen.types.StandardError;
import com.sun.fates.graphql.mappers.PersonMapper;
import com.sun.fates.graphql.mappers.PlaceMapper;
import com.sun.fates.model.PersonEntity;
import com.sun.fates.model.PlaceEntity;
import com.sun.fates.service.PersonService;
import com.sun.fates.service.PlaceService;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL business logic for people and places.
 */
@Service
public class FatesGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(FatesGraphQLService.class);

  private final PersonService personService;
  private final PlaceService placeService;
  private final PersonMapper personMapper;
  private final PlaceMapper placeMapper;

  public FatesGraphQLService(PersonService personService, PlaceService placeService,
      PersonMapper personMapper, PlaceMapper placeMapper) {
    this.personService = personService;
    this.placeService = placeService;
    this.personMapper = personMapper;
    this.placeMapper = placeMapper;
  }

  /**
   * Locates a person by id.
   *
   * @param id the person id
   * @return the GraphQL Person, or null if not found
   */
  @Transactional(readOnly = true)
  public Person person(String id) {
    return personService.locate(UUID.fromString(id))
        .map(personMapper::map)
        .orElse(null);
  }

  /**
   * Lists all people.
   *
   * @return a list of GraphQL Person objects
   */
  @Transactional(readOnly = true)
  public List<Person> listPeople() {
    return personService.findAll().stream()
        .map(personMapper::map)
        .collect(Collectors.toList());
  }

  /**
   * Locates a place by id.
   *
   * @param id the place id
   * @return the GraphQL Place, or null if not found
   */
  @Transactional(readOnly = true)
  public Place place(String id) {
    return placeService.locate(UUID.fromString(id))
        .map(placeMapper::map)
        .orElse(null);
  }

  /**
   * Lists all places.
   *
   * @return a list of GraphQL Place objects
   */
  @Transactional(readOnly = true)
  public List<Place> listPlaces() {
    return placeService.findAll().stream()
        .map(placeMapper::map)
        .collect(Collectors.toList());
  }

  /**
   * Creates a new person.
   *
   * @param input the person input
   * @return the result of the create operation
   */
  @Transactional
  public QueryResult createPerson(PersonInput input) {
    return mutate("createPerson", () -> {
      PersonEntity entity = new PersonEntity();
      personMapper.map(input, entity);
      return personService.save(entity).getId();
    });
  }

  /**
   * Updates an existing person.
   *
   * @param input the person input
   * @return the result of the update operation
   */
  @Transactional
  public QueryResult savePerson(PersonInput input) {
    return mutate("savePerson", () -> {
      PersonEntity entity = personService.findById(UUID.fromString(input.getId()))
          .orElseThrow(() -> new IllegalArgumentException("Person not found: " + input.getId()));
      personMapper.map(input, entity);
      return personService.save(entity).getId();
    });
  }

  /**
   * Deletes a person.
   *
   * @param id the person id
   * @return the result of the delete operation
   */
  @Transactional
  public QueryResult deletePerson(String id) {
    return mutate("deletePerson", () -> {
      personService.deleteById(UUID.fromString(id));
      return UUID.fromString(id);
    });
  }

  /**
   * Creates a new place.
   *
   * @param input the place input
   * @return the result of the create operation
   */
  @Transactional
  public QueryResult createPlace(PlaceInput input) {
    return mutate("createPlace", () -> {
      PlaceEntity entity = new PlaceEntity();
      placeMapper.map(input, entity);
      return placeService.save(entity).getId();
    });
  }

  /**
   * Updates an existing place.
   *
   * @param input the place input
   * @return the result of the update operation
   */
  @Transactional
  public QueryResult savePlace(PlaceInput input) {
    return mutate("savePlace", () -> {
      PlaceEntity entity = placeService.findById(UUID.fromString(input.getId()))
          .orElseThrow(() -> new IllegalArgumentException("Place not found: " + input.getId()));
      placeMapper.map(input, entity);
      return placeService.save(entity).getId();
    });
  }

  /**
   * Deletes a place.
   *
   * @param id the place id
   * @return the result of the delete operation
   */
  @Transactional
  public QueryResult deletePlace(String id) {
    return mutate("deletePlace", () -> {
      placeService.deleteById(UUID.fromString(id));
      return UUID.fromString(id);
    });
  }

  private QueryResult mutate(String op, Supplier<UUID> action) {
    try {
      UUID id = action.get();
      logger.info("{} succeeded for id {}", op, id);
      return QuerySuccess.newBuilder()
          .message(op + " succeeded")
          .id(id != null ? id.toString() : null)
          .build();
    } catch (Exception e) {
      logger.error("{} failed", op, e);
      return StandardError.newBuilder()
          .message(op + " failed: " + e.getMessage())
          .build();
    }
  }
}
