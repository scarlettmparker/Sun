package com.sun.fates.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sun.fates.codegen.types.Person;
import com.sun.fates.codegen.types.QueryResult;
import com.sun.fates.codegen.types.QuerySuccess;
import com.sun.fates.graphql.mappers.PersonMapper;
import com.sun.fates.graphql.mappers.PlaceMapper;
import com.sun.fates.model.PersonEntity;
import com.sun.fates.service.PersonService;
import com.sun.fates.service.PlaceService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FatesGraphQLServiceTest {

  @Mock private PersonService personService;
  @Mock private PlaceService placeService;
  @Mock private PersonMapper personMapper;
  @Mock private PlaceMapper placeMapper;

  @InjectMocks private FatesGraphQLService service;

  @Test
  void person_returnsMappedPersonWhenFound() {
    UUID id = UUID.randomUUID();
    PersonEntity entity = new PersonEntity();
    entity.setId(id);
    when(personService.locate(id)).thenReturn(Optional.of(entity));
    when(personMapper.map(entity)).thenReturn(Person.newBuilder().id(id.toString()).build());

    assertThat(service.person(id.toString())).isNotNull();
  }

  @Test
  void person_returnsNullWhenNotFound() {
    UUID id = UUID.randomUUID();
    when(personService.locate(id)).thenReturn(Optional.empty());

    assertThat(service.person(id.toString())).isNull();
  }

  @Test
  void listPeople_returnsMappedList() {
    PersonEntity entity = new PersonEntity();
    entity.setId(UUID.randomUUID());
    when(personService.findAll()).thenReturn(List.of(entity));
    when(personMapper.map(entity)).thenReturn(Person.newBuilder().id(entity.getId().toString()).build());

    assertThat(service.listPeople()).hasSize(1);
  }

  @Test
  void createPerson_returnsSuccessWithId() {
    PersonEntity saved = new PersonEntity();
    saved.setId(UUID.randomUUID());
    when(personService.save(any(PersonEntity.class))).thenReturn(saved);

    QueryResult result = service.createPerson(
        com.sun.fates.codegen.types.PersonInput.newBuilder().firstName("A").lastName("B").build());

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(saved.getId().toString());
  }

  @Test
  void deletePerson_returnsSuccessWithId() {
    UUID id = UUID.randomUUID();

    QueryResult result = service.deletePerson(id.toString());

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(id.toString());
  }
}
