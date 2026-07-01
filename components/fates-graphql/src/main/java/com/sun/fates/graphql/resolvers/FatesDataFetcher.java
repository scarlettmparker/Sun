package com.sun.fates.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.fates.codegen.types.FatesMutations;
import com.sun.fates.codegen.types.FatesQueries;
import com.sun.fates.codegen.types.Person;
import com.sun.fates.codegen.types.PersonInput;
import com.sun.fates.codegen.types.Place;
import com.sun.fates.codegen.types.PlaceInput;
import com.sun.fates.codegen.types.QueryResult;
import com.sun.fates.graphql.services.FatesGraphQLService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Data fetchers for the people queries and mutations.
 */
@DgsComponent
public class FatesDataFetcher {

  @Autowired
  private FatesGraphQLService fatesGraphQLService;

  /**
   * Provides the people queries object.
   *
   * @return the FatesQueries builder
   */
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
  @DgsData(parentType = "FatesQueries", field = "person")
  public Person person(String id) {
    return fatesGraphQLService.person(id);
  }

  /**
   * Lists all people.
   *
   * @return a list of Person objects
   */
  @DgsData(parentType = "FatesQueries", field = "listPeople")
  public List<Person> listPeople() {
    return fatesGraphQLService.listPeople();
  }

  /**
   * Locates a place by id.
   *
   * @param id the place id
   * @return the Place object
   */
  @DgsData(parentType = "FatesQueries", field = "place")
  public Place place(String id) {
    return fatesGraphQLService.place(id);
  }

  /**
   * Lists all places.
   *
   * @return a list of Place objects
   */
  @DgsData(parentType = "FatesQueries", field = "listPlaces")
  public List<Place> listPlaces() {
    return fatesGraphQLService.listPlaces();
  }

  /**
   * Provides the people mutations object.
   *
   * @return the FatesMutations builder
   */
  @DgsData(parentType = "Mutation", field = "fatesMutations")
  public FatesMutations getFatesMutations() {
    return FatesMutations.newBuilder().build();
  }

  /**
   * Creates a new person.
   *
   * @param input the person input
   * @return the result of the create operation
   */
  @DgsData(parentType = "FatesMutations", field = "createPerson")
  public QueryResult createPerson(PersonInput input) {
    return fatesGraphQLService.createPerson(input);
  }

  /**
   * Updates an existing person.
   *
   * @param input the person input
   * @return the result of the update operation
   */
  @DgsData(parentType = "FatesMutations", field = "savePerson")
  public QueryResult savePerson(PersonInput input) {
    return fatesGraphQLService.savePerson(input);
  }

  /**
   * Deletes a person.
   *
   * @param id the person id
   * @return the result of the delete operation
   */
  @DgsData(parentType = "FatesMutations", field = "deletePerson")
  public QueryResult deletePerson(String id) {
    return fatesGraphQLService.deletePerson(id);
  }

  /**
   * Creates a new place.
   *
   * @param input the place input
   * @return the result of the create operation
   */
  @DgsData(parentType = "FatesMutations", field = "createPlace")
  public QueryResult createPlace(PlaceInput input) {
    return fatesGraphQLService.createPlace(input);
  }

  /**
   * Updates an existing place.
   *
   * @param input the place input
   * @return the result of the update operation
   */
  @DgsData(parentType = "FatesMutations", field = "savePlace")
  public QueryResult savePlace(PlaceInput input) {
    return fatesGraphQLService.savePlace(input);
  }

  /**
   * Deletes a place.
   *
   * @param id the place id
   * @return the result of the delete operation
   */
  @DgsData(parentType = "FatesMutations", field = "deletePlace")
  public QueryResult deletePlace(String id) {
    return fatesGraphQLService.deletePlace(id);
  }
}
