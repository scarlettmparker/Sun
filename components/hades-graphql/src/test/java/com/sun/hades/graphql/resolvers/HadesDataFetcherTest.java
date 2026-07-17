package com.sun.hades.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sun.hades.codegen.types.AnnotationInput;
import com.sun.hades.codegen.types.QueryResult;
import com.sun.hades.codegen.types.QuerySuccess;
import com.sun.hades.codegen.types.ReaderText;
import com.sun.hades.codegen.types.ReaderTextInput;
import com.sun.hades.graphql.services.HadesGraphQLService;
import com.sun.hades.model.enums.CefrLevel;
import com.sun.hades.model.enums.ReaderTextType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HadesDataFetcherTest {

  @Mock
  private HadesGraphQLService service;

  @InjectMocks
  private HadesDataFetcher fetcher;

  @Test
  void texts_shouldDelegateToService() {
    ReaderText text = ReaderText.newBuilder().id("1").title("Title").build();
    when(service.texts(null)).thenReturn(
        com.sun.hades.codegen.types.PagedReaderTexts.newBuilder()
            .items(List.of(text)).pageInfo(
                com.sun.hades.codegen.types.PageInfo.newBuilder()
                    .page(0).size(1).totalPages(1).totalCount(1)
                    .hasNextPage(false).hasPreviousPage(false).build())
            .build());

    List<ReaderText> result = fetcher.texts(null).getItems();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTitle()).isEqualTo("Title");
  }

  @Test
  void createAnnotation_shouldDelegateToService() {
    AnnotationInput input = AnnotationInput.newBuilder()
        .textId("1").startOffset(0).endOffset(10).body("body").build();
    QueryResult mockResult = QuerySuccess.newBuilder().message("ok").id("2").build();
    when(service.createAnnotation("1", 0, 10, "body")).thenReturn(mockResult);

    QueryResult result = fetcher.createAnnotation(input);

    assertThat(result).isEqualTo(mockResult);
  }

  @Test
  void createText_shouldDelegateToService() {
    ReaderTextInput input = ReaderTextInput.newBuilder()
        .title("Title").content("content").language("fr")
        .level(CefrLevel.A1).build();
    QueryResult mockResult = QuerySuccess.newBuilder().message("ok").build();
    when(service.createText(input)).thenReturn(mockResult);

    QueryResult result = fetcher.createText(input);

    assertThat(result).isEqualTo(mockResult);
  }
}
