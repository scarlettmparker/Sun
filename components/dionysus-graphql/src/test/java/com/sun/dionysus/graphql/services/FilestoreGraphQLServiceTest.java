package com.sun.dionysus.graphql.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.sun.dionysus.codegen.types.Bucket;
import com.sun.dionysus.codegen.types.File;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class FilestoreGraphQLServiceTest {

  @Mock
  private RestTemplate restTemplate;

  @Mock
  private S3Client s3Client;

  @InjectMocks
  private FilestoreGraphQLService filestoreGraphQLService;

    @Test
    void health_returnsResponseFromRestApi() {
        String expected = "{\"status\":\"ok\"}";
        when(restTemplate.getForObject("https://filestore.scarlettparker.co.uk/api/health", String.class))
            .thenReturn(expected);

        String result = filestoreGraphQLService.health();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void listBuckets_returnsBucketsFromRestApi() {
        Bucket b = new Bucket();
        b.setId("b1");
        Bucket[] arr = {b};
        when(restTemplate.exchange("https://filestore.scarlettparker.co.uk/api/v2/ListBuckets", HttpMethod.GET, any(), eq(Bucket[].class)))
            .thenReturn(new ResponseEntity<>(arr, HttpStatus.OK));

        List<Bucket> result = filestoreGraphQLService.listBuckets();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("b1");
    }

    @Test
    void listFiles_returnsFiles() {
        S3Object obj = S3Object.builder().key("hello.txt").size(12L).build();
        ListObjectsV2Response resp = ListObjectsV2Response.builder().contents(obj).build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(resp);

        List<File> result = filestoreGraphQLService.listFiles("default-bucket");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getKey()).isEqualTo("hello.txt");
    }

    @Test
    void putFile_returnsTrue() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
          .thenReturn(PutObjectResponse.builder().build());

        boolean result = filestoreGraphQLService.putFile("default-bucket", "test.txt", "Hello");

        assertThat(result).isTrue();
        verify(s3Client).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
    }

    @Test
    void deleteFile_returnsTrue() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
          .thenReturn(DeleteObjectResponse.builder().build());

        boolean result = filestoreGraphQLService.deleteFile("default-bucket", "test.txt");

        assertThat(result).isTrue();
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void listFiles_emptyBucket_returnsEmpty() {
        ListObjectsV2Response resp = ListObjectsV2Response.builder().build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(resp);

        List<File> result = filestoreGraphQLService.listFiles("empty");

        assertThat(result).isEmpty();
    }

    @Test
    void startMultipartUpload_returnsUploadId() {
        CreateMultipartUploadResponse resp = CreateMultipartUploadResponse.builder().uploadId("upload123").build();
        when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class))).thenReturn(resp);

        String id = filestoreGraphQLService.startMultipartUpload("bucket", "bigfile.zip");

        assertThat(id).isEqualTo("upload123");
    }

    @Test
    void uploadPart_returnsEtag() {
        UploadPartResponse resp = UploadPartResponse.builder().eTag("\"etag-part1\"").build();
        when(s3Client.uploadPart(any(UploadPartRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class))).thenReturn(resp);

        String etag = filestoreGraphQLService.uploadPart("bucket", "bigfile.zip", "upload123", 1, "chunk1");

        assertThat(etag).isEqualTo("\"etag-part1\"");
    }

    @Test
    void completeMultipartUpload_returnsTrue() {
        CompletedPart cp = new CompletedPart();
        cp.setPartNumber(1);
        cp.setEtag("\"etag-part1\"");
        when(s3Client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
          .thenReturn(CompleteMultipartUploadResponse.builder().build());

        boolean ok = filestoreGraphQLService.completeMultipartUpload("bucket", "bigfile.zip", "upload123", List.of(cp));

        assertThat(ok).isTrue();
    }
}
