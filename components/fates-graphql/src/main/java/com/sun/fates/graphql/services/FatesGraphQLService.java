package com.sun.fates.graphql.services;

import com.sun.base.error.MutationException;
import com.sun.fates.codegen.types.CreatePersonResponse;
import com.sun.fates.codegen.types.CreatePlaceResponse;
import com.sun.fates.codegen.types.DeletePersonResponse;
import com.sun.fates.codegen.types.DeletePlaceResponse;
import com.sun.fates.codegen.types.Person;
import com.sun.fates.codegen.types.PersonInput;
import com.sun.fates.codegen.types.Place;
import com.sun.fates.codegen.types.PlaceInput;
import com.sun.fates.codegen.types.SavePersonResponse;
import com.sun.fates.codegen.types.SavePlaceResponse;
import com.sun.fates.graphql.mappers.PersonMapper;
import com.sun.fates.graphql.mappers.PlaceMapper;
import com.sun.fates.model.PersonEntity;
import com.sun.fates.model.PlaceEntity;
import com.sun.fates.service.PersonService;
import com.sun.fates.service.PlaceService;
import java.util.List;
import java.util.UUID;
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
   * @return the created person
   */
  @Transactional
  public CreatePersonResponse createPerson(PersonInput input) {
    try {
      PersonEntity entity = new PersonEntity();
      personMapper.map(input, entity);
      PersonEntity saved = personService.save(entity);
      logger.info("createPerson succeeded for id {}", saved.getId());
      return CreatePersonResponse.newBuilder()
          .message("createPerson succeeded")
          .person(personMapper.map(saved))
          .build();
    } catch (Exception e) {
      logger.error("createPerson failed", e);
      throw new MutationException("createPerson failed: " + e.getMessage(), e);
    }
  }

  /**
   * Updates an existing person.
   *
   * @param input the person input
   * @return the updated person
   */
  @Transactional
  public SavePersonResponse savePerson(PersonInput input) {
    try {
      PersonEntity entity = personService.findById(UUID.fromString(input.getId()))
          .orElseThrow(() -> new IllegalArgumentException("Person not found: " + input.getId()));
      personMapper.map(input, entity);
      PersonEntity saved = personService.save(entity);
      logger.info("savePerson succeeded for id {}", saved.getId());
      return SavePersonResponse.newBuilder()
          .message("savePerson succeeded")
          .person(personMapper.map(saved))
          .build();
    } catch (Exception e) {
      logger.error("savePerson failed", e);
      throw new MutationException("savePerson failed: " + e.getMessage(), e);
    }
  }

  /**
   * Deletes a person.
   *
   * @param id the person id
   * @return the deleted person id
   */
  @Transactional
  public DeletePersonResponse deletePerson(String id) {
    try {
      UUID personId = UUID.fromString(id);
      personService.deleteById(personId);
      logger.info("deletePerson succeeded for id {}", personId);
      return DeletePersonResponse.newBuilder()
          .message("deletePerson succeeded")
          .id(personId.toString())
          .build();
    } catch (Exception e) {
      logger.error("deletePerson failed", e);
      throw new MutationException("deletePerson failed: " + e.getMessage(), e);
    }
  }

  /**
   * Creates a new place.
   *
   * @param input the place input
   * @return the created place
   */
  @Transactional
  public CreatePlaceResponse createPlace(PlaceInput input) {
    try {
      PlaceEntity entity = new PlaceEntity();
      placeMapper.map(input, entity);
      PlaceEntity saved = placeService.save(entity);
      logger.info("createPlace succeeded for id {}", saved.getId());
      return CreatePlaceResponse.newBuilder()
          .message("createPlace succeeded")
          .place(placeMapper.map(saved))
          .build();
    } catch (Exception e) {
      logger.error("createPlace failed", e);
      throw new MutationException("createPlace failed: " + e.getMessage(), e);
    }
  }

  /**
   * Updates an existing place.
   *
   * @param input the place input
   * @return the updated place
   */
  @Transactional
  public SavePlaceResponse savePlace(PlaceInput input) {
    try {
      PlaceEntity entity = placeService.findById(UUID.fromString(input.getId()))
          .orElseThrow(() -> new IllegalArgumentException("Place not found: " + input.getId()));
      placeMapper.map(input, entity);
      PlaceEntity saved = placeService.save(entity);
      logger.info("savePlace succeeded for id {}", saved.getId());
      return SavePlaceResponse.newBuilder()
          .message("savePlace succeeded")
          .place(placeMapper.map(saved))
          .build();
    } catch (Exception e) {
      logger.error("savePlace failed", e);
      throw new MutationException("savePlace failed: " + e.getMessage(), e);
    }
  }

  /**
   * Deletes a place.
   *
   * @param id the place id
   * @return the deleted place id
   */
  @Transactional
  public DeletePlaceResponse deletePlace(String id) {
    try {
      UUID placeId = UUID.fromString(id);
      placeService.deleteById(placeId);
      logger.info("deletePlace succeeded for id {}", placeId);
      return DeletePlaceResponse.newBuilder()
          .message("deletePlace succeeded")
          .id(placeId.toString())
          .build();
    } catch (Exception e) {
      logger.error("deletePlace failed", e);
      throw new MutationException("deletePlace failed: " + e.getMessage(), e);
    }
  }
}
