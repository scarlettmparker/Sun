package com.sun.hades.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.hades.codegen.types.PagedPrivateNotes;
import com.sun.hades.codegen.types.PaginationInput;
import com.sun.hades.codegen.types.PrivateNoteInput;
import com.sun.hades.codegen.types.QueryResult;
import com.sun.hades.codegen.types.QuerySuccess;
import com.sun.hades.codegen.types.ShareNotesInput;
import com.sun.hades.graphql.services.PrivateNoteGraphQLService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrivateNoteDataFetcherTest {

  @Mock private PrivateNoteGraphQLService privateNoteGraphQLService;

  @InjectMocks private PrivateNoteDataFetcher fetcher;

  @Test
  void privateNotes_shouldDelegateToService() {
    PagedPrivateNotes page = PagedPrivateNotes.newBuilder().items(List.of()).build();
    PaginationInput pagination = PaginationInput.newBuilder().page(0).size(10).build();
    when(privateNoteGraphQLService.privateNotes("text-id", pagination)).thenReturn(page);

    PagedPrivateNotes result = fetcher.privateNotes("text-id", pagination);

    assertThat(result).isEqualTo(page);
    verify(privateNoteGraphQLService).privateNotes("text-id", pagination);
  }

  @Test
  void createPrivateNote_shouldDelegateToService() {
    PrivateNoteInput input = PrivateNoteInput.newBuilder()
        .textId("text-id").startOffset(0).endOffset(10).body("body").build();
    QueryResult mockResult = QuerySuccess.newBuilder().message("ok").id("id").build();
    when(privateNoteGraphQLService.createPrivateNote(input)).thenReturn(mockResult);

    QueryResult result = fetcher.createPrivateNote(input);

    assertThat(result).isEqualTo(mockResult);
    verify(privateNoteGraphQLService).createPrivateNote(input);
  }

  @Test
  void deletePrivateNote_shouldDelegateToService() {
    QueryResult mockResult = QuerySuccess.newBuilder().message("ok").id("id").build();
    when(privateNoteGraphQLService.deletePrivateNote("id")).thenReturn(mockResult);

    QueryResult result = fetcher.deletePrivateNote("id");

    assertThat(result).isEqualTo(mockResult);
    verify(privateNoteGraphQLService).deletePrivateNote("id");
  }

  @Test
  void shareNotes_shouldDelegateToService() {
    ShareNotesInput input = ShareNotesInput.newBuilder().textId("text-id").build();
    QueryResult mockResult = QuerySuccess.newBuilder().message("ok").id("text-id").build();
    when(privateNoteGraphQLService.shareNotes(input)).thenReturn(mockResult);

    QueryResult result = fetcher.shareNotes(input);

    assertThat(result).isEqualTo(mockResult);
    verify(privateNoteGraphQLService).shareNotes(input);
  }
}
