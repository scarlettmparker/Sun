package com.sun.dionysus.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.dionysus.graphql.services.FilestoreGraphQLService;
import com.sun.dionysus.codegen.types.FilestoreQueries;
import com.sun.dionysus.codegen.types.FilestoreMutations;
import com.sun.dionysus.codegen.types.RenameKeyResult;
import com.sun.dionysus.codegen.types.Bucket;
import com.sun.dionysus.codegen.types.KeyEntry;
import com.sun.dionysus.codegen.types.KeyDetail;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import com.sun.dionysus.codegen.types.BucketKeyInput;
import com.sun.dionysus.codegen.types.PresignInput;
import com.sun.dionysus.codegen.types.PutKeyInput;
import com.sun.dionysus.codegen.types.RenameKeyInput;

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
  @PreAuthorize("@permissions.has('graphql.dionysus.listBuckets')")
  public List<Bucket> listBuckets() {
    return filestoreGraphQLService.listBuckets();
  }

  @DgsData(parentType = "FilestoreQueries", field = "listKeys")
  @PreAuthorize("@permissions.has('graphql.dionysus.listKeys')")
  public List<KeyEntry> listKeys(String bucket, String prefix) {
    return filestoreGraphQLService.listKeys(bucket, prefix);
  }

  @DgsData(parentType = "FilestoreQueries", field = "locate")
  @PreAuthorize("@permissions.has('graphql.dionysus.locate')")
  public KeyDetail locate(String bucket, String keyPath) {
    return filestoreGraphQLService.locate(bucket, keyPath);
  }

  @DgsData(parentType = "FilestoreQueries", field = "listImages")
  @PreAuthorize("@permissions.has('graphql.dionysus.listImages')")
  public List<KeyDetail> listImages(String bucket) {
    return filestoreGraphQLService.listImages(bucket);
  }

  @DgsData(parentType = "FilestoreQueries", field = "locateImage")
  @PreAuthorize("@permissions.has('graphql.dionysus.locateImage')")
  public KeyDetail locateImage(String bucket, String keyPath) {
    return filestoreGraphQLService.locateImage(bucket, keyPath);
  }

  @DgsData(parentType = "Mutation", field = "filestoreMutations")
  public FilestoreMutations getFilestoreMutations() {
    return FilestoreMutations.newBuilder().build();
  }

  @DgsData(parentType = "FilestoreMutations", field = "putKey")
  @PreAuthorize("@permissions.has('graphql.dionysus.putKey')")
  public boolean putKey(PutKeyInput input) {
    return filestoreGraphQLService.putKey(input.getBucket(), input.getKey());
  }

  @DgsData(parentType = "FilestoreMutations", field = "deleteFile")
  @PreAuthorize("@permissions.has('graphql.dionysus.deleteFile')")
  public boolean deleteFile(BucketKeyInput input) {
    return filestoreGraphQLService.deleteFile(input.getBucket(), input.getKey());
  }

  @DgsData(parentType = "FilestoreMutations", field = "deleteKey")
  @PreAuthorize("@permissions.has('graphql.dionysus.deleteKey')")
  public boolean deleteKey(BucketKeyInput input) {
    return filestoreGraphQLService.deleteKey(input.getBucket(), input.getKey());
  }

  @DgsData(parentType = "FilestoreMutations", field = "renameKey")
  @PreAuthorize("@permissions.has('graphql.dionysus.renameKey')")
  public RenameKeyResult renameKey(RenameKeyInput input) {
    return filestoreGraphQLService.renameKey(input.getBucket(), input.getSourceKey(), input.getTargetKey(), input.getMerge());
  }

  @DgsData(parentType = "FilestoreMutations", field = "getPresignedUploadUrl")
  @PreAuthorize("@permissions.has('graphql.dionysus.getPresignedUploadUrl')")
  public String getPresignedUploadUrl(PresignInput input) {
    return filestoreGraphQLService.getPresignedUploadUrl(input.getBucket(), input.getKey(), input.getContentType());
  }

  @DgsData(parentType = "FilestoreMutations", field = "getPresignedUploadUrls")
  @PreAuthorize("@permissions.has('graphql.dionysus.getPresignedUploadUrls')")
  public List<String> getPresignedUploadUrls(
      List<PresignInput> input) {
    return filestoreGraphQLService.getPresignedUploadUrls(input);
  }

  @DgsData(parentType = "FilestoreMutations", field = "getPresignedDownloadUrl")
  @PreAuthorize("@permissions.has('graphql.dionysus.getPresignedDownloadUrl')")
  public String getPresignedDownloadUrl(BucketKeyInput input) {
    return filestoreGraphQLService.getPresignedDownloadUrl(input.getBucket(), input.getKey());
  }
}
