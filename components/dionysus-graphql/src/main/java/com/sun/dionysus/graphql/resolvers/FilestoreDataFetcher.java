package com.sun.dionysus.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.dionysus.graphql.services.FilestoreGraphQLService;
import com.sun.dionysus.codegen.types.FilestoreQueries;
import com.sun.dionysus.codegen.types.FilestoreMutations;
import com.sun.dionysus.codegen.types.RenameKeyResult;
import com.sun.dionysus.codegen.types.Bucket;
import com.sun.dionysus.codegen.types.File;
import com.sun.dionysus.codegen.types.KeyEntry;
import com.sun.dionysus.codegen.types.KeyDetail;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

@DgsComponent
public class FilestoreDataFetcher {

  @Autowired
  private FilestoreGraphQLService filestoreGraphQLService;

  @DgsData(parentType = "Query", field = "filestoreQueries")
  public FilestoreQueries getFilestoreQueries() {
    return FilestoreQueries.newBuilder().build();
  }

  @DgsData(parentType = "FilestoreQueries", field = "health")
  public String health() {
    return filestoreGraphQLService.health();
  }

  @DgsData(parentType = "FilestoreQueries", field = "listBuckets")
  public List<Bucket> listBuckets() {
    return filestoreGraphQLService.listBuckets();
  }

  @DgsData(parentType = "FilestoreQueries", field = "listKeys")
  public List<KeyEntry> listKeys(String bucket, String prefix) {
    return filestoreGraphQLService.listKeys(bucket, prefix);
  }

  @DgsData(parentType = "FilestoreQueries", field = "locate")
  public KeyDetail locate(String bucket, String keyPath) {
    return filestoreGraphQLService.locate(bucket, keyPath);
  }

  @DgsData(parentType = "FilestoreQueries", field = "listImages")
  public List<KeyDetail> listImages(String bucket) {
    return filestoreGraphQLService.listImages(bucket);
  }

  @DgsData(parentType = "FilestoreQueries", field = "locateImage")
  public KeyDetail locateImage(String bucket, String keyPath) {
    return filestoreGraphQLService.locateImage(bucket, keyPath);
  }

  @DgsData(parentType = "Mutation", field = "filestoreMutations")
  public FilestoreMutations getFilestoreMutations() {
    return FilestoreMutations.newBuilder().build();
  }

  @DgsData(parentType = "FilestoreMutations", field = "putKey")
  public boolean putKey(String bucket, String key) {
    return filestoreGraphQLService.putKey(bucket, key);
  }

  @DgsData(parentType = "FilestoreMutations", field = "deleteFile")
  public boolean deleteFile(String bucket, String key) {
    return filestoreGraphQLService.deleteFile(bucket, key);
  }

  @DgsData(parentType = "FilestoreMutations", field = "deleteKey")
  public boolean deleteKey(String bucket, String key) {
    return filestoreGraphQLService.deleteKey(bucket, key);
  }

  @DgsData(parentType = "FilestoreMutations", field = "renameKey")
  public RenameKeyResult renameKey(String bucket, String sourceKey, String targetKey, boolean merge) {
    return filestoreGraphQLService.renameKey(bucket, sourceKey, targetKey, merge);
  }

  @DgsData(parentType = "FilestoreMutations", field = "getPresignedUploadUrl")
  public String getPresignedUploadUrl(String bucket, String key, String contentType) {
    return filestoreGraphQLService.getPresignedUploadUrl(bucket, key, contentType);
  }

  @DgsData(parentType = "FilestoreMutations", field = "getPresignedDownloadUrl")
  public String getPresignedDownloadUrl(String bucket, String key) {
    return filestoreGraphQLService.getPresignedDownloadUrl(bucket, key);
  }
}
