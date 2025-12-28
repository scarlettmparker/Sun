package com.sun.cerberus.graphql.services;

import com.sun.cerberus.graphql.mappers.GalleryItemMapper;
import com.sun.cerberus.service.CerberusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.sun.cerberus.model.GalleryItemEntity;
import com.sun.cerberus.codegen.types.GalleryItem;
import com.sun.cerberus.codegen.types.GalleryItemInput;
import com.sun.cerberus.codegen.types.QueryResult;
import com.sun.cerberus.codegen.types.QuerySuccess;
import com.sun.cerberus.codegen.types.StandardError;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class GalleryGraphQLServiceTest {

  @Mock
  private CerberusService cerberusService;

  @Mock
  private GalleryItemMapper galleryItemMapper;

  @InjectMocks
  private GalleryGraphQLService galleryGraphQLService;

  private GalleryItemEntity galleryItemEntity1;
  private GalleryItemEntity galleryItemEntity2;
  private GalleryItem galleryItem1;
  private GalleryItem galleryItem2;

  @BeforeEach
  void setUp() {
    galleryItemEntity1 = new GalleryItemEntity();
    galleryItemEntity1.setId(UUID.randomUUID());
    galleryItemEntity1.setTitle("Test Gallery Item 1");
    galleryItemEntity1.setDescription("Description 1");
    galleryItemEntity1.setContent("Content 1");
    galleryItemEntity1.setImagePath("/path1.jpg");
    galleryItemEntity1.setForeignObject(Arrays.asList("id1", "id2"));

    galleryItemEntity2 = new GalleryItemEntity();
    galleryItemEntity2.setId(UUID.randomUUID());
    galleryItemEntity2.setTitle("Test Gallery Item 2");
    galleryItemEntity2.setDescription("Description 2");
    galleryItemEntity2.setContent("Content 2");
    galleryItemEntity2.setImagePath("/path2.jpg");
    galleryItemEntity2.setForeignObject(Arrays.asList());

    galleryItem1 = GalleryItem.newBuilder()
        .id(galleryItemEntity1.getId().toString())
        .title("Test Gallery Item 1")
        .description("Description 1")
        .content("Content 1")
        .imagePath("/path1.jpg")
        .foreignObject(Arrays.asList("id1", "id2"))
        .build();

    galleryItem2 = GalleryItem.newBuilder()
        .id(galleryItemEntity2.getId().toString())
        .title("Test Gallery Item 2")
        .description("Description 2")
        .content("Content 2")
        .imagePath("/path2.jpg")
        .foreignObject(Arrays.asList())
        .build();
  }

  @Test
  void list() {
    List<GalleryItemEntity> galleryItemEntities = Arrays.asList(galleryItemEntity1, galleryItemEntity2);
    when(cerberusService.list()).thenReturn(galleryItemEntities);
    when(galleryItemMapper.map(galleryItemEntity1)).thenReturn(galleryItem1);
    when(galleryItemMapper.map(galleryItemEntity2)).thenReturn(galleryItem2);
    List<GalleryItem> result = galleryGraphQLService.list();
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getTitle()).isEqualTo("Test Gallery Item 1");
    assertThat(result.get(0).getDescription()).isEqualTo("Description 1");
    assertThat(result.get(0).getContent()).isEqualTo("Content 1");
    assertThat(result.get(0).getImagePath()).isEqualTo("/path1.jpg");
    assertThat(result.get(0).getForeignObject()).containsExactly("id1", "id2");
    assertThat(result.get(1).getTitle()).isEqualTo("Test Gallery Item 2");
    assertThat(result.get(1).getDescription()).isEqualTo("Description 2");
    assertThat(result.get(1).getContent()).isEqualTo("Content 2");
    assertThat(result.get(1).getImagePath()).isEqualTo("/path2.jpg");
    assertThat(result.get(1).getForeignObject()).isEmpty();
  }

  @Test
  void list_shouldReturnEmptyListWhenNoItems() {
    when(cerberusService.list()).thenReturn(Arrays.asList());
    List<GalleryItem> result = galleryGraphQLService.list();
    assertThat(result).isEmpty();
  }

  @Test
  void locate() {
    when(cerberusService.locate(galleryItemEntity1.getId()))
        .thenReturn(java.util.Optional.of(galleryItemEntity1));
    when(galleryItemMapper.map(galleryItemEntity1)).thenReturn(galleryItem1);
    GalleryItem result = galleryGraphQLService.locate(galleryItemEntity1.getId().toString());
    assertThat(result.getTitle()).isEqualTo("Test Gallery Item 1");
    assertThat(result.getDescription()).isEqualTo("Description 1");
    assertThat(result.getContent()).isEqualTo("Content 1");
    assertThat(result.getImagePath()).isEqualTo("/path1.jpg");
    assertThat(result.getForeignObject()).containsExactly("id1", "id2");
  }

  @Test
  void locate_shouldThrowExceptionWhenItemNotFound() {
    when(cerberusService.locate(galleryItemEntity1.getId())).thenReturn(java.util.Optional.empty());
    assertThatThrownBy(() -> galleryGraphQLService.locate(galleryItemEntity1.getId().toString()))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Gallery item not found with id: " + galleryItemEntity1.getId().toString());
  }

  @Test
  void create() {
    GalleryItemInput input = GalleryItemInput.newBuilder()
        .title("New Gallery Item")
        .description("New Description")
        .content("New Content")
        .imagePath("/newpath.jpg")
        .foreignObject(Arrays.asList("new", "ids"))
        .build();

    GalleryItemEntity galleryItemEntity = new GalleryItemEntity();
    galleryItemEntity.setTitle("New Gallery Item");
    galleryItemEntity.setDescription("New Description");
    galleryItemEntity.setContent("New Content");
    galleryItemEntity.setImagePath("/newpath.jpg");
    galleryItemEntity.setForeignObject(Arrays.asList("new", "ids"));

    GalleryItemEntity savedEntity = new GalleryItemEntity();
    savedEntity.setId(UUID.randomUUID());
    savedEntity.setTitle("New Gallery Item");
    savedEntity.setDescription("New Description");
    savedEntity.setContent("New Content");
    savedEntity.setImagePath("/newpath.jpg");
    savedEntity.setForeignObject(Arrays.asList("new", "ids"));

    when(galleryItemMapper.mapInput(input)).thenReturn(galleryItemEntity);
    when(cerberusService.save(galleryItemEntity)).thenReturn(savedEntity);

    QueryResult result = galleryGraphQLService.create(input);

    assertThat(result).isInstanceOf(QuerySuccess.class);
    QuerySuccess success = (QuerySuccess) result;
    assertThat(success.getSuccess()).isTrue();
  }

  @Test
  void create_shouldReturnStandardErrorWhenExceptionOccurs() {
    GalleryItemInput input = GalleryItemInput.newBuilder()
        .title("New Gallery Item")
        .description("New Description")
        .content("New Content")
        .imagePath("/newpath.jpg")
        .foreignObject(Arrays.asList("new", "ids"))
        .build();

    GalleryItemEntity galleryItemEntity = new GalleryItemEntity();
    galleryItemEntity.setTitle("New Gallery Item");
    galleryItemEntity.setDescription("New Description");
    galleryItemEntity.setContent("New Content");
    galleryItemEntity.setImagePath("/newpath.jpg");
    galleryItemEntity.setForeignObject(Arrays.asList("new", "ids"));

    when(galleryItemMapper.mapInput(input)).thenReturn(galleryItemEntity);
    doThrow(new RuntimeException("Database error")).when(cerberusService).save(galleryItemEntity);

    QueryResult result = galleryGraphQLService.create(input);

    assertThat(result).isInstanceOf(StandardError.class);
    StandardError error = (StandardError) result;
    assertThat(error.getMessage()).contains("Failed to create gallery item: Database error");
    assertThat(error.getId()).isNull();
  }
}