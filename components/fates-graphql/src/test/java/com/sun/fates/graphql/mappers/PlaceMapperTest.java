package com.sun.fates.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.fates.codegen.types.Place;
import com.sun.fates.codegen.types.PlaceInput;
import com.sun.fates.model.PlaceEntity;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaceMapperTest {

  private final PlaceMapper mapper = new PlaceMapper();

  @Test
  void map_entityToGraphQL() {
    UUID id = UUID.randomUUID();

    PlaceEntity entity = new PlaceEntity();
    entity.setId(id);
    entity.setLine1("123 Main St");
    entity.setLine2("Apt 4");
    entity.setCity("London");
    entity.setRegion("England");
    entity.setPostalCode("SW1A 1AA");
    entity.setCountry("UK");

    Place result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getLine1()).isEqualTo("123 Main St");
    assertThat(result.getLine2()).isEqualTo("Apt 4");
    assertThat(result.getCity()).isEqualTo("London");
    assertThat(result.getRegion()).isEqualTo("England");
    assertThat(result.getPostalCode()).isEqualTo("SW1A 1AA");
    assertThat(result.getCountry()).isEqualTo("UK");
  }

  @Test
  void map_inputMergesOntoEntity() {
    PlaceInput input = PlaceInput.newBuilder()
        .line1("456 Oak Ave")
        .city("Manchester")
        .build();
    PlaceEntity entity = new PlaceEntity();

    mapper.map(input, entity);

    assertThat(entity.getLine1()).isEqualTo("456 Oak Ave");
    assertThat(entity.getCity()).isEqualTo("Manchester");
  }
}
