package com.sun.hades.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.hades.codegen.types.AddCommentResponse;
import com.sun.hades.codegen.types.CommentInput;
import com.sun.hades.codegen.types.DeleteCommentResponse;
import com.sun.hades.codegen.types.EditCommentResponse;
import com.sun.hades.codegen.types.PagedReaderComments;
import com.sun.hades.codegen.types.PaginationInput;
import com.sun.hades.codegen.types.ReaderComment;
import com.sun.hades.graphql.services.ReaderCommentGraphQLService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReaderCommentDataFetcherTest {

  @Mock private ReaderCommentGraphQLService readerCommentGraphQLService;

  @InjectMocks private ReaderCommentDataFetcher fetcher;

  @Test
  void comments_shouldDelegateToService() {
    PagedReaderComments page = PagedReaderComments.newBuilder().items(List.of()).build();
    PaginationInput pagination = PaginationInput.newBuilder().page(0).size(10).build();
    when(readerCommentGraphQLService.comments("ann-id", false, pagination)).thenReturn(page);

    PagedReaderComments result = fetcher.comments("ann-id", false, pagination);

    assertThat(result).isEqualTo(page);
    verify(readerCommentGraphQLService).comments("ann-id", false, pagination);
  }

  @Test
  void addComment_shouldDelegateToService() {
    CommentInput input = CommentInput.newBuilder().annotationId("ann-id").body("body").build();
    AddCommentResponse mockResult = AddCommentResponse.newBuilder()
        .message("ok")
        .comment(ReaderComment.newBuilder().id("id").body("body").build())
        .build();
    when(readerCommentGraphQLService.addComment(input)).thenReturn(mockResult);

    AddCommentResponse result = fetcher.addComment(input);

    assertThat(result).isEqualTo(mockResult);
    verify(readerCommentGraphQLService).addComment(input);
  }

  @Test
  void editComment_shouldDelegateToService() {
    EditCommentResponse mockResult = EditCommentResponse.newBuilder()
        .message("ok")
        .comment(ReaderComment.newBuilder().id("id").body("new").build())
        .build();
    when(readerCommentGraphQLService.editComment("id", "new")).thenReturn(mockResult);

    EditCommentResponse result = fetcher.editComment("id", "new");

    assertThat(result).isEqualTo(mockResult);
    verify(readerCommentGraphQLService).editComment("id", "new");
  }

  @Test
  void deleteComment_shouldDelegateToService() {
    DeleteCommentResponse mockResult = DeleteCommentResponse.newBuilder()
        .message("ok")
        .id("id")
        .build();
    when(readerCommentGraphQLService.deleteComment("id")).thenReturn(mockResult);

    DeleteCommentResponse result = fetcher.deleteComment("id");

    assertThat(result).isEqualTo(mockResult);
    verify(readerCommentGraphQLService).deleteComment("id");
  }
}
