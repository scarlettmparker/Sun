package com.sun.fates.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sun.fates.codegen.types.Person;
import com.sun.fates.codegen.types.QueryResult;
import com.sun.fates.codegen.types.QuerySuccess;
import com.sun.fates.graphql.services.FatesGraphQLService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FatesDataFetcherTest {

  @Mock private FatesGraphQLService service;

  @InjectMocks private FatesDataFetcher fetcher;

  @Test
  void person_delegatesToService() {
    UUID id = UUID.randomUUID();
    Person person = Person.newBuilder().id(id.toString()).build();
    when(service.person(id.toString())).thenReturn(person);

    assertThat(fetcher.person(id.toString())).isEqualTo(person);
  }

  @Test
  void listPeople_delegatesToService() {
    when(service.listPeople()).thenReturn(List.of());

    assertThat(fetcher.listPeople()).isEmpty();
  }

  @Test
  void createPerson_delegatesToService() {
    QuerySuccess success = QuerySuccess.newBuilder().message("ok").build();
    when(service.createPerson(any())).thenReturn(success);

    QueryResult result = fetcher.createPerson(null);

    assertThat(result).isSameAs(success);
  }

  private static <T> T any() {
    return org.mockito.ArgumentMatchers.any();
  }
}
