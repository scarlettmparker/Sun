package com.sun.dionysus.graphql.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.sun.dionysus.codegen.types.Bucket;
import com.sun.dionysus.codegen.types.File;
import com.sun.dionysus.codegen.types.KeyEntry;
import com.sun.dionysus.codegen.types.KeyDetail;
import com.sun.dionysus.codegen.types.RenameKeyResult;
import com.sun.dionysus.graphql.mappers.FileMapper;
import com.sun.dionysus.graphql.mappers.KeyEntryMapper;
import com.sun.dionysus.graphql.mappers.KeyDetailMapper;
import com.sun.dionysus.graphql.models.KeyDetailEntity;
import com.sun.dionysus.graphql.models.Status;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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

    @Mock
    private KeyDetailMapper keyDetailMapper;

    @Mock
    private com.sun.dionysus.service.KeyDetailService keyDetailService;

    @InjectMocks
    private FilestoreGraphQLService filestoreGraphQLService;

    @BeforeEach
    void setup() {
        when(keyDetailService.listActiveForBucketAndPath(anyString(), anyString())).thenReturn(List.of());
        when(keyDetailService.createOrUpdateDetail(anyString(), anyString(), any(), any())).thenAnswer(invocation -> {
            KeyDetailEntity d = new KeyDetailEntity();
            d.setBucket(invocation.getArgument(0));
            d.setKeyPath(invocation.getArgument(1));
            Object n = invocation.getArgument(2);
            d.setName(n == null ? null : n.toString());
            return d;
        });
        when(keyDetailService.locateByBucketAndKeyPath(anyString(), anyString())).thenReturn(Optional.empty());
    }

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
    void putKey_withExplicitName_createsDirectoryAsIs() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        boolean result = filestoreGraphQLService.putKey("default-bucket", "explicit-folder");

        assertThat(result).isTrue();
        
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(software.amazon.awssdk.core.sync.RequestBody.class));
        
        // Assert the key was formatted correctly
        assertThat(captor.getValue().key()).isEqualTo("explicit-folder/");
        verify(s3Client, times(0)).listObjectsV2(any(ListObjectsV2Request.class));
    }

    @Test
    void putKey_withNullKey_noConflict_createsRootNewKey() {
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder().build());
                
        when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        boolean result = filestoreGraphQLService.putKey("default-bucket", null);

        assertThat(result).isTrue();
        
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(software.amazon.awssdk.core.sync.RequestBody.class));
        
        assertThat(captor.getValue().key()).isEqualTo("new-key/");
    }

    @Test
    void putKey_withEmptyString_noConflict_createsRootNewKey() {
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder().build());
                
        when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        boolean result = filestoreGraphQLService.putKey("default-bucket", "   ");

        assertThat(result).isTrue();
        
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(software.amazon.awssdk.core.sync.RequestBody.class));
        
        assertThat(captor.getValue().key()).isEqualTo("new-key/");
    }

    @Test
    void putKey_withParentPath_hasConflicts_createsIncrementedNewKey() {
        ListObjectsV2Response hitResponse0 = ListObjectsV2Response.builder()
                .contents(S3Object.builder().key("parent/new-key/").build())
                .build();
        ListObjectsV2Response hitResponse1 = ListObjectsV2Response.builder()
                .contents(S3Object.builder().key("parent/new-key 1/").build())
                .build();
        ListObjectsV2Response emptyResponse = ListObjectsV2Response.builder().build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(hitResponse0)
                .thenReturn(hitResponse1)
                .thenReturn(emptyResponse);
                
        when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        boolean result = filestoreGraphQLService.putKey("default-bucket", "parent/");

        assertThat(result).isTrue();
        
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(software.amazon.awssdk.core.sync.RequestBody.class));
        assertThat(captor.getValue().key()).isEqualTo("parent/new-key 2/");
        verify(s3Client, times(3)).listObjectsV2(any(ListObjectsV2Request.class));
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
        dirEntry.setName("My Directory");
        dirEntry.setDescription("Directory description");

        KeyEntry fileEntry = new KeyEntry();
        fileEntry.setKey("dir/file.txt");
        fileEntry.setIsDirectory(false);
        fileEntry.setSize(42);
        fileEntry.setLastModified("2024-01-01T10:00:00Z");
        fileEntry.setName("My File");
        fileEntry.setDescription("File description");

        KeyDetailEntity dirDetail = new KeyDetailEntity();
        dirDetail.setName("My Directory");
        dirDetail.setDescription("Directory description");

        KeyDetailEntity fileDetail = new KeyDetailEntity();
        fileDetail.setName("My File");
        fileDetail.setDescription("File description");

        when(keyDetailService.listActiveForBucketAndPath("bucket", "dir/"))
                .thenReturn(List.of(dirDetail, fileDetail));
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(resp);
        when(keyEntryMapper.mapDirectory(any(CommonPrefix.class), any(KeyDetailEntity.class))).thenReturn(dirEntry);
        when(keyEntryMapper.mapFile(any(S3Object.class), any(KeyDetailEntity.class))).thenReturn(fileEntry);

        List<KeyEntry> result = filestoreGraphQLService.listKeys("bucket", "dir/");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getKey()).isEqualTo("dir/");
        assertThat(result.get(0).getIsDirectory()).isTrue();
        assertThat(result.get(0).getName()).isEqualTo("My Directory");
        assertThat(result.get(0).getDescription()).isEqualTo("Directory description");
        assertThat(result.get(1).getKey()).isEqualTo("dir/file.txt");
        assertThat(result.get(1).getIsDirectory()).isFalse();
        assertThat(result.get(1).getSize()).isEqualTo(42);
        assertThat(result.get(1).getName()).isEqualTo("My File");
        assertThat(result.get(1).getDescription()).isEqualTo("File description");
    }

    @Test
    void listKeys_withoutKeyDetail_returnsEntriesWithoutMetadata() {
        CommonPrefix prefix = CommonPrefix.builder().prefix("folder/").build();
        S3Object file = S3Object.builder()
                .key("folder/file.txt")
                .size(100L)
                .build();
        ListObjectsV2Response resp = ListObjectsV2Response.builder()
                .commonPrefixes(prefix)
                .contents(file)
                .build();

        KeyEntry dirEntry = new KeyEntry();
        dirEntry.setKey("folder/");
        dirEntry.setIsDirectory(true);
        // name and description are null

        KeyEntry fileEntry = new KeyEntry();
        fileEntry.setKey("folder/file.txt");
        fileEntry.setIsDirectory(false);
        fileEntry.setSize(100);
        // name and description are null

        when(keyDetailService.listActiveForBucketAndPath("bucket", "folder/"))
                .thenReturn(List.of()); // No KeyDetail records
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(resp);
        when(keyEntryMapper.mapDirectory(any(CommonPrefix.class))).thenReturn(dirEntry);
        when(keyEntryMapper.mapFile(any(S3Object.class))).thenReturn(fileEntry);

        List<KeyEntry> result = filestoreGraphQLService.listKeys("bucket", "folder/");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getKey()).isEqualTo("folder/");
        assertThat(result.get(0).getName()).isNull();
        assertThat(result.get(0).getDescription()).isNull();
        assertThat(result.get(1).getKey()).isEqualTo("folder/file.txt");
        assertThat(result.get(1).getName()).isNull();
        assertThat(result.get(1).getDescription()).isNull();
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

    @Test
    void renameKey_noSourceObjects_returnsFalse() {
        ListObjectsV2Response emptyResponse = ListObjectsV2Response.builder().build();
        when(s3Client.listObjectsV2Paginator(any(Consumer.class))).thenReturn(new io.awssdk.core.pagination.sync.SdkIterable<ListObjectsV2Response>() {
            @Override
            public java.util.Iterator<ListObjectsV2Response> iterator() {
                return List.of(emptyResponse).iterator();
            }
        });

        RenameKeyResult result = filestoreGraphQLService.renameKey("bucket", "old.txt", "new.txt", false);

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getHasConflicts()).isFalse();
        assertThat(result.getConflicts()).isEmpty();
    }

    @Test
    void renameKey_withoutMergeAndHasConflicts_returnsConflicts() {
        S3Object sourceObj = S3Object.builder().key("old.txt").build();
        ListObjectsV2Response listResponse = ListObjectsV2Response.builder().contents(sourceObj).build();
        
        when(s3Client.listObjectsV2Paginator(any(Consumer.class))).thenReturn(new io.awssdk.core.pagination.sync.SdkIterable<ListObjectsV2Response>() {
            @Override
            public java.util.Iterator<ListObjectsV2Response> iterator() {
                return List.of(listResponse).iterator();
            }
        });
        
        // Mock headObject to throw nothing (indicating the destination file ALREADY exists)
        when(s3Client.headObject(any(Consumer.class))).thenReturn(HeadObjectResponse.builder().build());

        RenameKeyResult result = filestoreGraphQLService.renameKey("bucket", "old.txt", "new.txt", false);

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getHasConflicts()).isTrue();
        assertThat(result.getConflicts()).containsExactly("new.txt");
    }

    @Test
    void renameKey_withMergeTrue_bypassesConflictsAndExecutesMove() {
        S3Object sourceObj = S3Object.builder().key("old.txt").build();
        ListObjectsV2Response listResponse = ListObjectsV2Response.builder().contents(sourceObj).build();
        
        when(s3Client.listObjectsV2Paginator(any(Consumer.class))).thenReturn(new io.awssdk.core.pagination.sync.SdkIterable<ListObjectsV2Response>() {
            @Override
            public java.util.Iterator<ListObjectsV2Response> iterator() {
                return List.of(listResponse).iterator();
            }
        });
        
        when(s3Client.copyObject(any(CopyObjectRequest.class))).thenReturn(CopyObjectResponse.builder().build());
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(DeleteObjectsResponse.builder().build());

        RenameKeyResult result = filestoreGraphQLService.renameKey("bucket", "old.txt", "new.txt", true);

        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getHasConflicts()).isFalse();
        verify(s3Client).copyObject(any(CopyObjectRequest.class));
        verify(s3Client).deleteObjects(any(DeleteObjectsRequest.class));
    }

    @Test
    void locate_returnsMappedKeyDetail() {
        KeyDetailEntity entity = new KeyDetailEntity();
        entity.setBucket("my-bucket");
        entity.setKeyPath("documents/report.pdf");
        entity.setName("Quarterly Report");
        entity.setDescription("Q4 financial summary");
        entity.setStatus(Status.ACTIVE);

        KeyDetail expected = new KeyDetail();
        expected.setBucket("my-bucket");
        expected.setKeyPath("documents/report.pdf");
        expected.setName("Quarterly Report");
        expected.setDescription("Q4 financial summary");
        expected.setStatus("ACTIVE");

        when(keyDetailService.locateByBucketAndKeyPath("my-bucket", "documents/report.pdf"))
                .thenReturn(Optional.of(entity));
        when(keyDetailMapper.map(entity)).thenReturn(expected);

        KeyDetail result = filestoreGraphQLService.locate("my-bucket", "documents/report.pdf");

        assertThat(result).isNotNull();
        assertThat(result.getBucket()).isEqualTo("my-bucket");
        assertThat(result.getKeyPath()).isEqualTo("documents/report.pdf");
        assertThat(result.getName()).isEqualTo("Quarterly Report");
        assertThat(result.getDescription()).isEqualTo("Q4 financial summary");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void locate_returnsNullWhenNotFound() {
        when(keyDetailService.locateByBucketAndKeyPath("my-bucket", "missing.txt"))
                .thenReturn(Optional.empty());

        KeyDetail result = filestoreGraphQLService.locate("my-bucket", "missing.txt");

        assertThat(result).isNull();
    }
}
