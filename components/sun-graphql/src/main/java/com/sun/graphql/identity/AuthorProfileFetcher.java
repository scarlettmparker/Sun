package com.sun.graphql.identity;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.sun.hades.codegen.types.PrivateNote;
import com.sun.hades.codegen.types.ReaderAccount;
import java.util.concurrent.CompletableFuture;
import org.dataloader.DataLoader;
import com.sun.hades.codegen.types.ReaderAnnotation;
import com.sun.hades.codegen.types.ReaderComment;
import com.sun.icarus.codegen.types.ForumPost;

/**
 * Gateway field resolvers that attach a reader-account profile to each
 * author-bearing type, batched through the readerProfile DataLoader.
 */
@DgsComponent
public class AuthorProfileFetcher {

  @DgsData(parentType = "ReaderAnnotation", field = "authorProfile")
  public CompletableFuture<ReaderAccount> annotationProfile(
      DgsDataFetchingEnvironment env) {
    ReaderAnnotation source = env.getSource();
    return loadById(env, source.getAuthor() == null ? null : source.getAuthor().getId());
  }

  @DgsData(parentType = "ReaderComment", field = "authorProfile")
  public CompletableFuture<ReaderAccount> commentProfile(DgsDataFetchingEnvironment env) {
    ReaderComment source = env.getSource();
    return loadById(env, source.getAuthor() == null ? null : source.getAuthor().getId());
  }

  @DgsData(parentType = "ForumPost", field = "authorProfile")
  public CompletableFuture<ReaderAccount> postProfile(DgsDataFetchingEnvironment env) {
    ForumPost source = env.getSource();
    return loadById(env, source.getAuthor() == null ? null : source.getAuthor().getId());
  }

  @DgsData(parentType = "PrivateNote", field = "authorProfile")
  public CompletableFuture<ReaderAccount> privateNoteProfile(
      DgsDataFetchingEnvironment env) {
    PrivateNote source = env.getSource();
    return loadById(env, source.getAuthor() == null ? null : source.getAuthor().getId());
  }

  private CompletableFuture<ReaderAccount> loadById(
      DgsDataFetchingEnvironment env, String discordId) {
    if (discordId == null) {
      return CompletableFuture.completedFuture(null);
    }
    DataLoader<String, ReaderAccount> loader = env.getDataLoader("readerProfile");
    return loader.load(discordId);
  }
}
