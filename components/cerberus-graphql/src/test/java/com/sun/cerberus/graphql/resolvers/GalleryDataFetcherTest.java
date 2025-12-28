package com.sun.cerberus.graphql.resolvers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sun.cerberus.graphql.services.GalleryGraphQLService;
import com.sun.cerberus.codegen.types.GalleryItem;
import com.sun.cerberus.codegen.types.GalleryItemInput;
import com.sun.cerberus.codegen.types.QueryResult;
import com.sun.cerberus.codegen.types.QuerySuccess;

@ExtendWith(MockitoExtension.class)
class GalleryDataFetcherTest {

  @Mock
  private GalleryGraphQLService galleryGraphQLService;

  @InjectMocks
  private GalleryDataFetcher galleryDataFetcher;

  private List<GalleryItem> mockGalleryItems;

  @BeforeEach
  void setUp() {
    GalleryItem galleryItem1 = GalleryItem.newBuilder()
        .id("1")
        .title("Test Gallery 1")
        .description("Description 1")
        .content("Content 1")
        .imagePath("/path1.jpg")
        .foreignObject(Arrays.asList("id1", "id2"))
        .build();

    GalleryItem galleryItem2 = GalleryItem.newBuilder()
        .id("2")
        .title("Test Gallery 2")
        .description("Description 2")
        .content("Content 2")
        .imagePath("/path2.jpg")
        .foreignObject(Arrays.asList("id3", "id4"))
        .build();

    mockGalleryItems = Arrays.asList(galleryItem1, galleryItem2);
  }

  @Test
  void list() {
    when(galleryGraphQLService.list()).thenReturn(mockGalleryItems);
    List<GalleryItem> result = galleryDataFetcher.list();
    assertThat(result).isEqualTo(mockGalleryItems);
    assertThat(result).hasSize(2);

    // First gallery item
    assertThat(result.get(0).getTitle()).isEqualTo("Test Gallery 1");
    assertThat(result.get(0).getDescription()).isEqualTo("Description 1");
    assertThat(result.get(0).getContent()).isEqualTo("Content 1");
    assertThat(result.get(0).getImagePath()).isEqualTo("/path1.jpg");
    assertThat(result.get(0).getForeignObject()).containsExactly("id1", "id2");

    // Second gallery item
    assertThat(result.get(1).getTitle()).isEqualTo("Test Gallery 2");
    assertThat(result.get(1).getDescription()).isEqualTo("Description 2");
    assertThat(result.get(1).getContent()).isEqualTo("Content 2");
    assertThat(result.get(1).getImagePath()).isEqualTo("/path2.jpg");
    assertThat(result.get(1).getForeignObject()).containsExactly("id3", "id4");
  }

  @Test
  void locate() {
    GalleryItem mockGallery = GalleryItem.newBuilder()
        .id("1")
        .title("Test Gallery 1")
        .description("Description 1")
        .content("Content 1")
        .imagePath("/path1.jpg")
        .foreignObject(Arrays.asList("id1", "id2"))
        .build();

    when(galleryGraphQLService.locate("1")).thenReturn(mockGallery);
    GalleryItem result = galleryDataFetcher.locate("1");
    assertThat(result).isEqualTo(mockGallery);

    assertThat(result.getTitle()).isEqualTo("Test Gallery 1");
    assertThat(result.getDescription()).isEqualTo("Description 1");
    assertThat(result.getContent()).isEqualTo("Content 1");
    assertThat(result.getImagePath()).isEqualTo("/path1.jpg");
    assertThat(result.getForeignObject()).containsExactly("id1", "id2");
  }

  @Test
  void create_shouldReturnQueryResultFromService() {
    GalleryItemInput input = GalleryItemInput.newBuilder()
        .title("New Title")
        .description("New Description")
        .content("New content")
        .imagePath("/newpath.jpg")
        .foreignObject(Arrays.asList("new", "ids"))
        .build();

    QueryResult mockResult = QuerySuccess.newBuilder().success(true).build();

    when(galleryGraphQLService.create(input)).thenReturn(mockResult);

    QueryResult result = galleryDataFetcher.create(input);

    assertThat(result).isEqualTo(mockResult);
  }
}