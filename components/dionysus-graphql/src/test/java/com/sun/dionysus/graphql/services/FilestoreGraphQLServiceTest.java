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
import com.sun.dionysus.codegen.types.KeyEntry;
import com.sun.dionysus.graphql.mappers.FileMapper;
import com.sun.dionysus.graphql.mappers.KeyEntryMapper;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class FilestoreGraphQLServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private S3Client s3Client;

    @Mock
    private FileMapper fileMapper;

    @Mock
    private KeyEntryMapper keyEntryMapper;

    @InjectMocks
    private FilestoreGraphQLService filestoreGraphQLService;

    @Test
    void health_returnsResponseFromRestApi() {
        String expected = "{\"status\":\"ok\"}";
        when(restTemplate.getForObject("https://filestore.int.scarlettparker.co.uk/api/health", String.class))
                .thenReturn(expected);

        String result = filestoreGraphQLService.health();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void listBuckets_returnsBucketsFromRestApi() {
        Bucket b = new Bucket();
        b.setId("b1");
        Bucket[] arr = { b };
        when(restTemplate.exchange("https://filestore.int.scarlettparker.co.uk/api/v2/ListBuckets", HttpMethod.GET, any(),
                eq(Bucket[].class)))
                .thenReturn(new ResponseEntity<>(arr, HttpStatus.OK));

        List<Bucket> result = filestoreGraphQLService.listBuckets();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("b1");
    }

    @Test
    void listFiles_returnsFiles() {
        S3Object obj = S3Object.builder().key("hello.txt").size(12L).build();
        com.sun.dionysus.codegen.types.File file = new com.sun.dionysus.codegen.types.File();
        file.setKey("hello.txt");
        file.setSize(12);

        ListObjectsV2Response resp = ListObjectsV2Response.builder().contents(obj).build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(resp);
        when(fileMapper.mapObject(any(S3Object.class))).thenReturn(file);

        List<File> result = filestoreGraphQLService.listFiles("default-bucket");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getKey()).isEqualTo("hello.txt");
        assertThat(result.get(0).getSize()).isEqualTo(12);
    }

    @Test
    void putFile_returnsTrue() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        boolean result = filestoreGraphQLService.putFile("default-bucket", "test.txt", "Hello", null);

        assertThat(result).isTrue();
        verify(s3Client).putObject(any(PutObjectRequest.class),
                any(software.amazon.awssdk.core.sync.RequestBody.class));
    }

    @Test
    void putKey_returnsTrue() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        boolean result = filestoreGraphQLService.putKey("default-bucket", "folder");

        assertThat(result).isTrue();
        verify(s3Client).putObject(any(PutObjectRequest.class),
                any(software.amazon.awssdk.core.sync.RequestBody.class));
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
        when(s3Client.uploadPart(any(UploadPartRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenReturn(resp);

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

    @Test
    void listKeys_returnsDirectoryAndFileEntries() {
        CommonPrefix prefix = CommonPrefix.builder().prefix("dir/").build();
        S3Object file = S3Object.builder()
                .key("dir/file.txt")
                .size(42L)
                .lastModified(Instant.parse("2024-01-01T10:00:00Z"))
                .build();
        ListObjectsV2Response resp = ListObjectsV2Response.builder()
                .commonPrefixes(prefix)
                .contents(file)
                .build();

        KeyEntry dirEntry = new KeyEntry();
        dirEntry.setKey("dir/");
        dirEntry.setIsDirectory(true);

        KeyEntry fileEntry = new KeyEntry();
        fileEntry.setKey("dir/file.txt");
        fileEntry.setIsDirectory(false);
        fileEntry.setSize(42);
        fileEntry.setLastModified("2024-01-01T10:00:00Z");

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(resp);
        when(keyEntryMapper.mapDirectory(any(CommonPrefix.class))).thenReturn(dirEntry);
        when(keyEntryMapper.mapFile(any(S3Object.class))).thenReturn(fileEntry);

        List<KeyEntry> result = filestoreGraphQLService.listKeys("bucket", "dir/");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getKey()).isEqualTo("dir/");
        assertThat(result.get(0).getIsDirectory()).isTrue();
        assertThat(result.get(1).getKey()).isEqualTo("dir/file.txt");
        assertThat(result.get(1).getIsDirectory()).isFalse();
        assertThat(result.get(1).getSize()).isEqualTo(42);
    }

    @Test
    void deleteKey_returnsTrue() {
        ListObjectsV2Response empty = ListObjectsV2Response.builder().build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(empty);
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class)))
                .thenReturn(DeleteObjectsResponse.builder().build());

        boolean result = filestoreGraphQLService.deleteKey("default-bucket", "folder/");

        assertThat(result).isTrue();
        verify(s3Client).deleteObjects(any(DeleteObjectsRequest.class));
    }

    @Test
    void deleteKey_deletesExactAndSubKeys() {
        S3Object marker = S3Object.builder().key("dir/").build();
        S3Object child = S3Object.builder().key("dir/child.txt").build();
        ListObjectsV2Response resp = ListObjectsV2Response.builder().contents(marker, child).build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(resp);
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class)))
                .thenReturn(DeleteObjectsResponse.builder().build());

        boolean result = filestoreGraphQLService.deleteKey("bucket", "dir");

        assertThat(result).isTrue();
        verify(s3Client).deleteObjects(any(DeleteObjectsRequest.class));
    }

    @Test
    void deleteKey_handlesPaginationAndBatch() {
        S3Object p1 = S3Object.builder().key("big/a").build();
        ListObjectsV2Response r1 = ListObjectsV2Response.builder()
                .contents(p1)
                .isTruncated(true)
                .nextContinuationToken("t1")
                .build();
        S3Object p2 = S3Object.builder().key("big/b").build();
        ListObjectsV2Response r2 = ListObjectsV2Response.builder().contents(p2).build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(r1, r2);
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class)))
                .thenReturn(DeleteObjectsResponse.builder().build());

        boolean result = filestoreGraphQLService.deleteKey("bkt", "big");

        assertThat(result).isTrue();
        verify(s3Client, times(2)).listObjectsV2(any(ListObjectsV2Request.class));
        verify(s3Client).deleteObjects(any(DeleteObjectsRequest.class));
    }

    @Test
    void deleteKey_withNoObjects_doesNotCallDeleteObjects() {
        ListObjectsV2Response empty = ListObjectsV2Response.builder().build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(empty);

        boolean result = filestoreGraphQLService.deleteKey("bucket", "emptykey");

        assertThat(result).isTrue();
        verify(s3Client, times(0)).deleteObjects(any(DeleteObjectsRequest.class));
    }
}
