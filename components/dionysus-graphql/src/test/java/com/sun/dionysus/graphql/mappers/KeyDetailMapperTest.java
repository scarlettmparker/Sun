package com.sun.dionysus.graphql.mappers;

import com.sun.dionysus.graphql.models.KeyDetail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for KeyDetailMapper.
 */
@ExtendWith(MockitoExtension.class)
class KeyDetailMapperTest {

  private KeyDetailMapper mapper = new KeyDetailMapper();

  @Test
  void map_withKeyDetail_returnsSameObject() {
    KeyDetail detail = new KeyDetail();
    detail.setName("Test File");
    detail.setDescription("Test Description");

    KeyDetail result = mapper.map(detail);

    assertThat(result).isSameAs(detail);
    assertThat(result.getName()).isEqualTo("Test File");
    assertThat(result.getDescription()).isEqualTo("Test Description");
  }

  @Test
  void map_withNull_returnsNull() {
    KeyDetail result = mapper.map(null);

    assertThat(result).isNull();
  }
}
