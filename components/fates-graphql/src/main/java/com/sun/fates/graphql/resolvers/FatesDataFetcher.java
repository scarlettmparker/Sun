package com.sun.fates.graphql.resolvers;
import com.sun.base.audit.annotation.Audited;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.fates.codegen.types.CreatePersonResponse;
import com.sun.fates.codegen.types.CreatePlaceResponse;
import com.sun.fates.codegen.types.DeletePersonResponse;
import com.sun.fates.codegen.types.DeletePlaceResponse;
import com.sun.fates.codegen.types.FatesMutations;
import com.sun.fates.codegen.types.FatesQueries;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import com.sun.base.audit.annotation.NotAudited;
/**
 * Data fetchers for the people queries and mutations.
 */
@DgsComponent
public class FatesDataFetcher {

  private static final Logger logger = LoggerFactory.getLogger(FatesDataFetcher.class);

  private final PersonService personService;
  private final PlaceService placeService;
  private final PersonMapper personMapper;
  private final PlaceMapper placeMapper;

  public FatesDataFetcher(PersonService personService, PlaceService placeService,
      PersonMapper personMapper, PlaceMapper placeMapper) {
    this.personService = personService;
    this.placeService = placeService;
    this.personMapper = personMapper;
    this.placeMapper = placeMapper;
  }

  /**
   * Provides the people queries object.
   *
   * @return the FatesQueries builder
   */
  @NotAudited(reason = "namespace root")
  @DgsData(parentType = "Query", field = "fatesQueries")
  public FatesQueries getFatesQueries() {
    return FatesQueries.newBuilder().build();
  }

  /**
   * Locates a person by id.
   *
   * @param id the person id
   * @return the Person object
   */
  @Audited(event = "PERSON")
  @DgsData(parentType = "FatesQueries", field = "person")
  @PreAuthorize("@permissions.has('graphql.fates.person')")
  @Transactional(readOnly = true)
  public Person person(String id) {
    return personService.locate(UUID.fromString(id))
        .map(personMapper::map)
        .orElse(null);
  }

  /**
   * Lists all people.
   *
   * @return a list of Person objects
   */
  @Audited(event = "LISTPEOPLE", entity = "People")
  @DgsData(parentType = "FatesQueries", field = "listPeople")
  @PreAuthorize("@permissions.has('graphql.fates.listPeople')")
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
   * @return the Place object
   */
  @Audited(event = "PLACE")
  @DgsData(parentType = "FatesQueries", field = "place")
  @PreAuthorize("@permissions.has('graphql.fates.place')")
  @Transactional(readOnly = true)
  public Place place(String id) {
    return placeService.locate(UUID.fromString(id))
        .map(placeMapper::map)
        .orElse(null);
  }

  /**
   * Lists all places.
   *
   * @return a list of Place objects
   */
  @Audited(event = "LISTPLACES", entity = "Place")
  @DgsData(parentType = "FatesQueries", field = "listPlaces")
  @PreAuthorize("@permissions.has('graphql.fates.listPlaces')")
  @Transactional(readOnly = true)
  public List<Place> listPlaces() {
    return placeService.findAll().stream()
        .map(placeMapper::map)
        .collect(Collectors.toList());
  }

  /**
   * Provides the people mutations object.
   *
   * @return the FatesMutations builder
   */
  @NotAudited(reason = "namespace root")
  @DgsData(parentType = "Mutation", field = "fatesMutations")
  public FatesMutations getFatesMutations() {
    return FatesMutations.newBuilder().build();
  }

  /**
   * Creates a new person.
   *
   * @param input the person input
   * @return the created person
   */
  @Audited(event = "CREATEPERSON", entity = "Person")
  @DgsData(parentType = "FatesMutations", field = "createPerson")
  @PreAuthorize("@permissions.has('graphql.fates.createPerson')")
  @Transactional
  public CreatePersonResponse createPerson(PersonInput input) {
    PersonEntity entity = new PersonEntity();
    personMapper.map(input, entity);
    PersonEntity saved = personService.save(entity);
    logger.info("createPerson succeeded for id {}", saved.getId());
    return CreatePersonResponse.newBuilder()
        .message("createPerson succeeded")
        .person(personMapper.map(saved))
        .build();
  }

  /**
   * Updates an existing person.
   *
   * @param input the person input
   * @return the updated person
   */
  @Audited(event = "SAVEPERSON", entity = "Person")
  @DgsData(parentType = "FatesMutations", field = "savePerson")
  @PreAuthorize("@permissions.has('graphql.fates.savePerson')")
  @Transactional
  public SavePersonResponse savePerson(PersonInput input) {
    PersonEntity entity = personService.findById(UUID.fromString(input.getId()))
        .orElseThrow(() -> new IllegalArgumentException("Person not found: " + input.getId()));
    personMapper.map(input, entity);
    PersonEntity saved = personService.save(entity);
    logger.info("savePerson succeeded for id {}", saved.getId());
    return SavePersonResponse.newBuilder()
        .message("savePerson succeeded")
        .person(personMapper.map(saved))
        .build();
  }

  /**
   * Deletes a person.
   *
   * @param id the person id
   * @return the deleted person id
   */
  @Audited(event = "DELETEPERSON", entity = "Person")
  @DgsData(parentType = "FatesMutations", field = "deletePerson")
  @PreAuthorize("@permissions.has('graphql.fates.deletePerson')")
  @Transactional
  public DeletePersonResponse deletePerson(String id) {
    UUID personId = UUID.fromString(id);
    personService.deleteById(personId);
    logger.info("deletePerson succeeded for id {}", personId);
    return DeletePersonResponse.newBuilder()
        .message("deletePerson succeeded")
        .id(personId.toString())
        .build();
  }

  /**
   * Creates a new place.
   *
   * @param input the place input
   * @return the created place
   */
  @Audited(event = "CREATEPLACE", entity = "Place")
  @DgsData(parentType = "FatesMutations", field = "createPlace")
  @PreAuthorize("@permissions.has('graphql.fates.createPlace')")
  @Transactional
  public CreatePlaceResponse createPlace(PlaceInput input) {
    PlaceEntity entity = new PlaceEntity();
    placeMapper.map(input, entity);
    PlaceEntity saved = placeService.save(entity);
    logger.info("createPlace succeeded for id {}", saved.getId());
    return CreatePlaceResponse.newBuilder()
        .message("createPlace succeeded")
        .place(placeMapper.map(saved))
        .build();
  }

  /**
   * Updates an existing place.
   *
   * @param input the place input
   * @return the updated place
   */
  @Audited(event = "SAVEPLACE", entity = "Place")
  @DgsData(parentType = "FatesMutations", field = "savePlace")
  @PreAuthorize("@permissions.has('graphql.fates.savePlace')")
  @Transactional
  public SavePlaceResponse savePlace(PlaceInput input) {
    PlaceEntity entity = placeService.findById(UUID.fromString(input.getId()))
        .orElseThrow(() -> new IllegalArgumentException("Place not found: " + input.getId()));
    placeMapper.map(input, entity);
    PlaceEntity saved = placeService.save(entity);
    logger.info("savePlace succeeded for id {}", saved.getId());
    return SavePlaceResponse.newBuilder()
        .message("savePlace succeeded")
        .place(placeMapper.map(saved))
        .build();
  }

  /**
   * Deletes a place.
   *
   * @param id the place id
   * @return the deleted place id
   */
  @Audited(event = "DELETEPLACE", entity = "Place")
  @DgsData(parentType = "FatesMutations", field = "deletePlace")
  @PreAuthorize("@permissions.has('graphql.fates.deletePlace')")
  @Transactional
  public DeletePlaceResponse deletePlace(String id) {
    UUID placeId = UUID.fromString(id);
    placeService.deleteById(placeId);
    logger.info("deletePlace succeeded for id {}", placeId);
    return DeletePlaceResponse.newBuilder()
        .message("deletePlace succeeded")
        .id(placeId.toString())
        .build();
  }
}
