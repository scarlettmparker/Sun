package com.sun.graphql.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.sun.hades.codegen.types.PrivateNote;
import com.sun.hades.codegen.types.ReaderAccount;
import com.sun.hades.codegen.types.ReaderAnnotation;
import com.sun.hades.codegen.types.ReaderComment;
import com.sun.icarus.codegen.types.ForumPost;
import com.sun.icarus.model.enums.PostStatus;
import java.util.concurrent.CompletableFuture;
import org.dataloader.DataLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class AuthorProfileFetcherTest {

  private final AuthorProfileFetcher fetcher = new AuthorProfileFetcher();

  @Test
  void annotationProfile_shouldLoadViaDataLoader() throws Exception {
    String discordId = "123";
    ReaderAccount account = ReaderAccount.newBuilder().id("1").discordId(discordId).build();
    DataLoader<String, ReaderAccount> loader = mock(DataLoader.class);
    when(loader.load(discordId)).thenReturn(CompletableFuture.completedFuture(account));

    com.sun.hades.codegen.types.RemoteUser author = com.sun.hades.codegen.types.RemoteUser.newBuilder()
        .type(com.sun.hades.codegen.types.RemoteUserType.DISCORD).id(discordId).build();
    ReaderAnnotation annotation = ReaderAnnotation.newBuilder().id("a1").author(author).build();
    DgsDataFetchingEnvironment env = mock(DgsDataFetchingEnvironment.class);
    when(env.getSource()).thenReturn(annotation);
    doReturn(loader).when(env).getDataLoader("readerProfile");

    CompletableFuture<ReaderAccount> result = fetcher.annotationProfile(env);

    assertThat(result.get()).isEqualTo(account);
    verify(loader).load(discordId);
  }

  @Test
  void annotationProfile_withNullAuthor_shouldReturnNull() throws Exception {
    ReaderAnnotation annotation = ReaderAnnotation.newBuilder().id("a1").build();
    DgsDataFetchingEnvironment env = mock(DgsDataFetchingEnvironment.class);
    when(env.getSource()).thenReturn(annotation);

    CompletableFuture<ReaderAccount> result = fetcher.annotationProfile(env);

    assertThat(result.get()).isNull();
  }

  @Test
  void commentProfile_shouldLoadViaDataLoader() throws Exception {
    String discordId = "456";
    ReaderAccount account = ReaderAccount.newBuilder().id("2").discordId(discordId).build();
    DataLoader<String, ReaderAccount> loader = mock(DataLoader.class);
    when(loader.load(discordId)).thenReturn(CompletableFuture.completedFuture(account));

    com.sun.hades.codegen.types.RemoteUser hadesAuthor = com.sun.hades.codegen.types.RemoteUser.newBuilder().type(com.sun.hades.codegen.types.RemoteUserType.DISCORD).id(discordId).build();
    ReaderComment comment = ReaderComment.newBuilder().id("c1").author(hadesAuthor).build();
    DgsDataFetchingEnvironment env = mock(DgsDataFetchingEnvironment.class);
    when(env.getSource()).thenReturn(comment);
    doReturn(loader).when(env).getDataLoader("readerProfile");

    CompletableFuture<ReaderAccount> result = fetcher.commentProfile(env);

    assertThat(result.get()).isEqualTo(account);
    verify(loader).load(discordId);
  }

  @Test
  void postProfile_shouldLoadViaDataLoader() throws Exception {
    String discordId = "789";
    ReaderAccount account = ReaderAccount.newBuilder().id("3").discordId(discordId).build();
    DataLoader<String, ReaderAccount> loader = mock(DataLoader.class);
    when(loader.load(discordId)).thenReturn(CompletableFuture.completedFuture(account));

    com.sun.icarus.codegen.types.RemoteUser author = com.sun.icarus.codegen.types.RemoteUser.newBuilder()
        .type(com.sun.icarus.codegen.types.RemoteUserType.DISCORD).id(discordId).build();
    ForumPost post = ForumPost.newBuilder().id("p1").threadId("t1").body("hi").status(PostStatus.ACTIVE)
        .upvotes(0).downvotes(0).netScore(0).author(author).build();
    DgsDataFetchingEnvironment env = mock(DgsDataFetchingEnvironment.class);
    when(env.getSource()).thenReturn(post);
    doReturn(loader).when(env).getDataLoader("readerProfile");

    CompletableFuture<ReaderAccount> result = fetcher.postProfile(env);

    assertThat(result.get()).isEqualTo(account);
    verify(loader).load(discordId);
  }

  @Test
  void privateNoteProfile_withNullAuthor_shouldReturnNull() throws Exception {
    PrivateNote note = PrivateNote.newBuilder().id("n1").build();
    DgsDataFetchingEnvironment env = mock(DgsDataFetchingEnvironment.class);
    when(env.getSource()).thenReturn(note);

    CompletableFuture<ReaderAccount> result = fetcher.privateNoteProfile(env);

    assertThat(result.get()).isNull();
  }
}
