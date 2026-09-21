package com.sun.dionysus.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.dionysus.codegen.types.Bucket;
import com.sun.dionysus.codegen.types.BucketKeyInput;
import com.sun.dionysus.codegen.types.DeleteFileResponse;
import com.sun.dionysus.codegen.types.DeleteKeyResponse;
import com.sun.dionysus.codegen.types.GetPresignedDownloadUrlResponse;
import com.sun.dionysus.codegen.types.GetPresignedUploadUrlResponse;
import com.sun.dionysus.codegen.types.GetPresignedUploadUrlsResponse;
import com.sun.dionysus.codegen.types.KeyDetail;
import com.sun.dionysus.codegen.types.KeyEntry;
import com.sun.dionysus.codegen.types.PresignInput;
import com.sun.dionysus.codegen.types.PutKeyInput;
import com.sun.dionysus.codegen.types.PutKeyResponse;
import com.sun.dionysus.codegen.types.RenameKeyInput;
import com.sun.dionysus.codegen.types.RenameKeyResponse;
import com.sun.dionysus.graphql.services.FilestoreGraphQLService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FilestoreDataFetcherTest {

  @Mock private FilestoreGraphQLService filestoreGraphQLService;
  @InjectMocks private FilestoreDataFetcher fetcher;

  @Test
  void health_shouldDelegateToService() {
    when(filestoreGraphQLService.health()).thenReturn("ok");

    String result = fetcher.health();

    assertThat(result).isEqualTo("ok");
    verify(filestoreGraphQLService).health();
  }

  @Test
  void listBuckets_shouldDelegateToService() {
    Bucket b = new Bucket();
    b.setId("b1");
    when(filestoreGraphQLService.listBuckets()).thenReturn(List.of(b));

    List<Bucket> result = fetcher.listBuckets();

    assertThat(result).hasSize(1);
    verify(filestoreGraphQLService).listBuckets();
  }

  @Test
  void listKeys_shouldDelegateToService() {
    KeyEntry entry = new KeyEntry();
    entry.setKey("a/");
    when(filestoreGraphQLService.listKeys("bucket", "prefix/")).thenReturn(List.of(entry));

    List<KeyEntry> result = fetcher.listKeys("bucket", "prefix/");

    assertThat(result).hasSize(1);
    verify(filestoreGraphQLService).listKeys("bucket", "prefix/");
  }

  @Test
  void locate_shouldDelegateToService() {
    KeyDetail detail = new KeyDetail();
    detail.setBucket("b");
    detail.setKeyPath("k");
    when(filestoreGraphQLService.locate("b", "k")).thenReturn(detail);

    KeyDetail result = fetcher.locate("b", "k");

    assertThat(result).isEqualTo(detail);
    verify(filestoreGraphQLService).locate("b", "k");
  }

  @Test
  void listImages_shouldDelegateToService() {
    KeyDetail d = new KeyDetail();
    when(filestoreGraphQLService.listImages("b")).thenReturn(List.of(d));

    List<KeyDetail> result = fetcher.listImages("b");

    assertThat(result).hasSize(1);
    verify(filestoreGraphQLService).listImages("b");
  }

  @Test
  void locateImage_shouldDelegateToService() {
    KeyDetail d = new KeyDetail();
    when(filestoreGraphQLService.locateImage("b", "k")).thenReturn(d);

    KeyDetail result = fetcher.locateImage("b", "k");

    assertThat(result).isEqualTo(d);
    verify(filestoreGraphQLService).locateImage("b", "k");
  }

  @Test
  void putKey_shouldDelegateToService() {
    PutKeyInput input = PutKeyInput.newBuilder().bucket("b").key("k").build();
    PutKeyResponse expected = PutKeyResponse.newBuilder().message("putKey succeeded").key("k").build();
    when(filestoreGraphQLService.putKey("b", "k")).thenReturn(expected);

    PutKeyResponse result = fetcher.putKey(input);

    assertThat(result).isEqualTo(expected);
    verify(filestoreGraphQLService).putKey("b", "k");
  }

  @Test
  void deleteFile_shouldDelegateToService() {
    BucketKeyInput input = BucketKeyInput.newBuilder().bucket("b").key("k").build();
    DeleteFileResponse expected = DeleteFileResponse.newBuilder().message("deleteFile succeeded").key("k").build();
    when(filestoreGraphQLService.deleteFile("b", "k")).thenReturn(expected);

    DeleteFileResponse result = fetcher.deleteFile(input);

    assertThat(result.getKey()).isEqualTo("k");
    verify(filestoreGraphQLService).deleteFile("b", "k");
  }

  @Test
  void deleteKey_shouldDelegateToService() {
    BucketKeyInput input = BucketKeyInput.newBuilder().bucket("b").key("k").build();
    DeleteKeyResponse expected = DeleteKeyResponse.newBuilder().message("deleteKey succeeded").key("k").build();
    when(filestoreGraphQLService.deleteKey("b", "k")).thenReturn(expected);

    DeleteKeyResponse result = fetcher.deleteKey(input);

    assertThat(result.getKey()).isEqualTo("k");
    verify(filestoreGraphQLService).deleteKey("b", "k");
  }

  @Test
  void renameKey_shouldDelegateToService() {
    RenameKeyInput input = RenameKeyInput.newBuilder().bucket("b").sourceKey("s").targetKey("t").merge(false).build();
    RenameKeyResponse expected = RenameKeyResponse.newBuilder()
        .message("renameKey succeeded").success(true).hasConflicts(false).conflicts(List.of()).build();
    when(filestoreGraphQLService.renameKey("b", "s", "t", false)).thenReturn(expected);

    RenameKeyResponse result = fetcher.renameKey(input);

    assertThat(result).isEqualTo(expected);
    verify(filestoreGraphQLService).renameKey("b", "s", "t", false);
  }

  @Test
  void getPresignedUploadUrl_shouldDelegateToService() {
    PresignInput input = PresignInput.newBuilder().bucket("b").key("k").contentType("text/plain").build();
    GetPresignedUploadUrlResponse expected = GetPresignedUploadUrlResponse.newBuilder()
        .message("getPresignedUploadUrl succeeded").url("url").build();
    when(filestoreGraphQLService.getPresignedUploadUrl("b", "k", "text/plain")).thenReturn(expected);

    GetPresignedUploadUrlResponse result = fetcher.getPresignedUploadUrl(input);

    assertThat(result.getUrl()).isEqualTo("url");
    verify(filestoreGraphQLService).getPresignedUploadUrl("b", "k", "text/plain");
  }

  @Test
  void getPresignedUploadUrls_shouldDelegateToService() {
    PresignInput i = PresignInput.newBuilder().bucket("b").key("k").build();
    GetPresignedUploadUrlsResponse expected = GetPresignedUploadUrlsResponse.newBuilder()
        .message("getPresignedUploadUrls succeeded").urls(List.of("u1")).build();
    when(filestoreGraphQLService.getPresignedUploadUrls(List.of(i))).thenReturn(expected);

    GetPresignedUploadUrlsResponse result = fetcher.getPresignedUploadUrls(List.of(i));

    assertThat(result.getUrls()).containsExactly("u1");
    verify(filestoreGraphQLService).getPresignedUploadUrls(List.of(i));
  }

  @Test
  void getPresignedDownloadUrl_shouldDelegateToService() {
    BucketKeyInput input = BucketKeyInput.newBuilder().bucket("b").key("k").build();
    GetPresignedDownloadUrlResponse expected = GetPresignedDownloadUrlResponse.newBuilder()
        .message("getPresignedDownloadUrl succeeded").url("dl").build();
    when(filestoreGraphQLService.getPresignedDownloadUrl("b", "k")).thenReturn(expected);

    GetPresignedDownloadUrlResponse result = fetcher.getPresignedDownloadUrl(input);

    assertThat(result.getUrl()).isEqualTo("dl");
    verify(filestoreGraphQLService).getPresignedDownloadUrl("b", "k");
  }
}
