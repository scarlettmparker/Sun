package com.sun.briareus.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.briareus.codegen.types.BlogGalleryItem;
import com.sun.cerberus.model.GalleryItemEntity;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BlogGalleryItemMapperTest {

  private final BlogGalleryItemMapper mapper = new BlogGalleryItemMapper();

  @Test
  void map_shouldMapAllFields() {
    GalleryItemEntity entity = new GalleryItemEntity();
    UUID id = UUID.randomUUID();
    entity.setId(id);
    entity.setTitle("title");
    entity.setImagePath("path/key.jpg");
    entity.setRemoteObject(List.of("briareus:post:1"));

    BlogGalleryItem result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getTitle()).isEqualTo("title");
    assertThat(result.getImagePath()).isEqualTo("path/key.jpg");
    assertThat(result.getRemoteObject()).containsExactly("briareus:post:1");
  }
}
