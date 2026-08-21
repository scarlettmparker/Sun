package com.sun.hades.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.hades.codegen.types.AnnotationInput;
import com.sun.hades.codegen.types.PagedReaderAnnotations;
import com.sun.hades.codegen.types.PaginationInput;
import com.sun.hades.codegen.types.QueryResult;
import com.sun.hades.codegen.types.QuerySuccess;
import com.sun.hades.codegen.types.ReaderAnnotation;
import com.sun.hades.graphql.services.ReaderAnnotationGraphQLService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReaderAnnotationDataFetcherTest {

  @Mock private ReaderAnnotationGraphQLService readerAnnotationGraphQLService;

  @InjectMocks private ReaderAnnotationDataFetcher fetcher;

  @Test
  void annotations_shouldDelegateToService() {
    PagedReaderAnnotations page = PagedReaderAnnotations.newBuilder().items(List.of()).build();
    PaginationInput pagination = PaginationInput.newBuilder().page(0).size(10).build();
    when(readerAnnotationGraphQLService.annotations("text-id", false, pagination)).thenReturn(page);

    PagedReaderAnnotations result = fetcher.annotations("text-id", false, pagination);

    assertThat(result).isEqualTo(page);
    verify(readerAnnotationGraphQLService).annotations("text-id", false, pagination);
  }

  @Test
  void annotation_shouldDelegateToService() {
    ReaderAnnotation annotation = ReaderAnnotation.newBuilder().id("id").body("body").build();
    when(readerAnnotationGraphQLService.annotation("id")).thenReturn(annotation);

    ReaderAnnotation result = fetcher.annotation("id");

    assertThat(result).isEqualTo(annotation);
    verify(readerAnnotationGraphQLService).annotation("id");
  }

  @Test
  void createAnnotation_shouldDelegateToService() {
    AnnotationInput input = AnnotationInput.newBuilder()
        .textId("text-id").startOffset(0).endOffset(10).body("body").build();
    QueryResult mockResult = QuerySuccess.newBuilder().message("ok").id("id").build();
    when(readerAnnotationGraphQLService.createAnnotation("text-id", 0, 10, "body")).thenReturn(mockResult);

    QueryResult result = fetcher.createAnnotation(input);

    assertThat(result).isEqualTo(mockResult);
    verify(readerAnnotationGraphQLService).createAnnotation("text-id", 0, 10, "body");
  }

  @Test
  void editAnnotation_shouldDelegateToService() {
    QueryResult mockResult = QuerySuccess.newBuilder().message("ok").id("id").build();
    when(readerAnnotationGraphQLService.editAnnotation("id", "new body")).thenReturn(mockResult);

    QueryResult result = fetcher.editAnnotation("id", "new body");

    assertThat(result).isEqualTo(mockResult);
    verify(readerAnnotationGraphQLService).editAnnotation("id", "new body");
  }

  @Test
  void deleteAnnotation_shouldDelegateToService() {
    QueryResult mockResult = QuerySuccess.newBuilder().message("ok").id("id").build();
    when(readerAnnotationGraphQLService.deleteAnnotation("id")).thenReturn(mockResult);

    QueryResult result = fetcher.deleteAnnotation("id");

    assertThat(result).isEqualTo(mockResult);
    verify(readerAnnotationGraphQLService).deleteAnnotation("id");
  }

  @Test
  void attachObject_shouldDelegateToService() {
    QueryResult mockResult = QuerySuccess.newBuilder().message("ok").id("id").build();
    when(readerAnnotationGraphQLService.attachObject("source", "target")).thenReturn(mockResult);

    QueryResult result = fetcher.attachObject("source", "target");

    assertThat(result).isEqualTo(mockResult);
    verify(readerAnnotationGraphQLService).attachObject("source", "target");
  }
}
