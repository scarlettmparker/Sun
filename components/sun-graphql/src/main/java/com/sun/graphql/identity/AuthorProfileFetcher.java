package com.sun.graphql.identity;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.sun.hades.codegen.types.ReaderAccount;
import java.util.concurrent.CompletableFuture;
import org.dataloader.DataLoader;

/**
 * Gateway field resolvers that attach a reader-account profile to each
 * author-bearing type, batched through the readerProfile DataLoader.
 */
@DgsComponent
public class AuthorProfileFetcher {

  @DgsData(parentType = "ReaderAnnotation", field = "authorProfile")
  public CompletableFuture<ReaderAccount> annotationProfile(
      DgsDataFetchingEnvironment env) {
    com.sun.hades.codegen.types.ReaderAnnotation source = env.getSource();
    return loadById(env, source.getAuthor() != null ? source.getAuthor().getId() : null);
  }

  @DgsData(parentType = "ReaderComment", field = "authorProfile")
  public CompletableFuture<ReaderAccount> commentProfile(DgsDataFetchingEnvironment env) {
    com.sun.hades.codegen.types.ReaderComment source = env.getSource();
    return loadById(env, source.getAuthor() != null ? source.getAuthor().getId() : null);
  }

  @DgsData(parentType = "ForumPost", field = "authorProfile")
  public CompletableFuture<ReaderAccount> postProfile(DgsDataFetchingEnvironment env) {
    com.sun.icarus.codegen.types.ForumPost source = env.getSource();
    return loadById(env, source.getAuthor() != null ? source.getAuthor().getId() : null);
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
