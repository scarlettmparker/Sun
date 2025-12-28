package com.sun.cerberus.graphql.mappers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.cerberus.model.GalleryItemEntity;
import com.sun.cerberus.codegen.types.GalleryItem;
import com.sun.cerberus.codegen.types.GalleryItemInput;

@ExtendWith(MockitoExtension.class)
class GalleryItemMapperTest {

  private GalleryItemMapper galleryItemMapper = new GalleryItemMapper();

  @Test
  void map() {
    LocalDateTime createdAt = LocalDateTime.of(2023, 1, 1, 10, 0);
    LocalDateTime updatedAt = LocalDateTime.of(2023, 1, 2, 11, 0);

    GalleryItemEntity galleryItemEntity = new GalleryItemEntity();
    galleryItemEntity.setId(UUID.randomUUID());
    galleryItemEntity.setTitle("Test Title");
    galleryItemEntity.setDescription("Test Description");
    galleryItemEntity.setContent("Test Content");
    galleryItemEntity.setImagePath("/path/to/image.jpg");
    galleryItemEntity.setForeignObject(Arrays.asList("id1", "id2"));
    galleryItemEntity.setCreatedAt(createdAt);
    galleryItemEntity.setLastUpdatedAt(updatedAt);

    GalleryItem result = galleryItemMapper.map(galleryItemEntity);

    assertThat(result.getId()).isEqualTo(galleryItemEntity.getId().toString());
    assertThat(result.getTitle()).isEqualTo("Test Title");
    assertThat(result.getDescription()).isEqualTo("Test Description");
    assertThat(result.getContent()).isEqualTo("Test Content");
    assertThat(result.getImagePath()).isEqualTo("/path/to/image.jpg");
    assertThat(result.getForeignObject()).containsExactly("id1", "id2");
    assertThat(result.getCreatedAt()).isEqualTo(createdAt);
    assertThat(result.getUpdatedAt()).isEqualTo(updatedAt);
  }

  @Test
  void mapInput() {
    GalleryItemInput input = GalleryItemInput.newBuilder()
        .title("Test Title")
        .description("Test Description")
        .content("Test Content")
        .imagePath("/path/to/image.jpg")
        .foreignObject(Arrays.asList("id1", "id2"))
        .build();

    GalleryItemEntity result = galleryItemMapper.mapInput(input);

    assertThat(result.getTitle()).isEqualTo("Test Title");
    assertThat(result.getDescription()).isEqualTo("Test Description");
    assertThat(result.getContent()).isEqualTo("Test Content");
    assertThat(result.getImagePath()).isEqualTo("/path/to/image.jpg");
    assertThat(result.getForeignObject()).containsExactly("id1", "id2");
  }
}