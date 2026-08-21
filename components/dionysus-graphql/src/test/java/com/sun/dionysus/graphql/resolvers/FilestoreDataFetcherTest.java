package com.sun.dionysus.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.dionysus.codegen.types.Bucket;
import com.sun.dionysus.codegen.types.BucketKeyInput;
import com.sun.dionysus.codegen.types.KeyDetail;
import com.sun.dionysus.codegen.types.KeyEntry;
import com.sun.dionysus.codegen.types.PresignInput;
import com.sun.dionysus.codegen.types.PutKeyInput;
import com.sun.dionysus.codegen.types.RenameKeyInput;
import com.sun.dionysus.codegen.types.RenameKeyResult;
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
    when(filestoreGraphQLService.putKey("b", "k")).thenReturn(true);

    boolean result = fetcher.putKey(input);

    assertThat(result).isTrue();
    verify(filestoreGraphQLService).putKey("b", "k");
  }

  @Test
  void deleteFile_shouldDelegateToService() {
    BucketKeyInput input = BucketKeyInput.newBuilder().bucket("b").key("k").build();
    when(filestoreGraphQLService.deleteFile("b", "k")).thenReturn(true);

    boolean result = fetcher.deleteFile(input);

    assertThat(result).isTrue();
    verify(filestoreGraphQLService).deleteFile("b", "k");
  }

  @Test
  void deleteKey_shouldDelegateToService() {
    BucketKeyInput input = BucketKeyInput.newBuilder().bucket("b").key("k").build();
    when(filestoreGraphQLService.deleteKey("b", "k")).thenReturn(true);

    boolean result = fetcher.deleteKey(input);

    assertThat(result).isTrue();
    verify(filestoreGraphQLService).deleteKey("b", "k");
  }

  @Test
  void renameKey_shouldDelegateToService() {
    RenameKeyInput input = RenameKeyInput.newBuilder().bucket("b").sourceKey("s").targetKey("t").merge(false).build();
    RenameKeyResult expected = new RenameKeyResult();
    expected.setSuccess(true);
    when(filestoreGraphQLService.renameKey("b", "s", "t", false)).thenReturn(expected);

    RenameKeyResult result = fetcher.renameKey(input);

    assertThat(result).isEqualTo(expected);
    verify(filestoreGraphQLService).renameKey("b", "s", "t", false);
  }

  @Test
  void getPresignedUploadUrl_shouldDelegateToService() {
    PresignInput input = PresignInput.newBuilder().bucket("b").key("k").contentType("text/plain").build();
    when(filestoreGraphQLService.getPresignedUploadUrl("b", "k", "text/plain")).thenReturn("url");

    String result = fetcher.getPresignedUploadUrl(input);

    assertThat(result).isEqualTo("url");
    verify(filestoreGraphQLService).getPresignedUploadUrl("b", "k", "text/plain");
  }

  @Test
  void getPresignedUploadUrls_shouldDelegateToService() {
    PresignInput i = PresignInput.newBuilder().bucket("b").key("k").build();
    when(filestoreGraphQLService.getPresignedUploadUrls(List.of(i))).thenReturn(List.of("u1"));

    List<String> result = fetcher.getPresignedUploadUrls(List.of(i));

    assertThat(result).containsExactly("u1");
    verify(filestoreGraphQLService).getPresignedUploadUrls(List.of(i));
  }

  @Test
  void getPresignedDownloadUrl_shouldDelegateToService() {
    BucketKeyInput input = BucketKeyInput.newBuilder().bucket("b").key("k").build();
    when(filestoreGraphQLService.getPresignedDownloadUrl("b", "k")).thenReturn("dl");

    String result = fetcher.getPresignedDownloadUrl(input);

    assertThat(result).isEqualTo("dl");
    verify(filestoreGraphQLService).getPresignedDownloadUrl("b", "k");
  }
}
