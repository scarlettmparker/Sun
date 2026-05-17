package com.sun.dionysus.graphql.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import com.sun.dionysus.codegen.types.Bucket;
import com.sun.dionysus.codegen.types.CompletedPart;
import com.sun.dionysus.codegen.types.File;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FilestoreGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(FilestoreGraphQLService.class);

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private S3Client s3Client;

  /**
   * Checks the health of the external filestore service.
   */
  public String health() {
    logger.info("Calling external health REST API");
    String url = "https://filestore.scarlettparker.co.uk/api/health";
    String response = restTemplate.getForObject(url, String.class);
    logger.info("Health response: {}", response);
    return response;
  }

  /**
   * Lists all available buckets via the REST API.
   */
  public List<Bucket> listBuckets() {
    logger.info("Calling ListBuckets REST API");
    String url = "https://filestore.scarlettparker.co.uk/api/v2/ListBuckets";
    ResponseEntity<Bucket[]> resp = authenticatedGet(url, Bucket[].class);
    Bucket[] buckets = resp.getBody();
    List<Bucket> result = buckets != null ? Arrays.asList(buckets) : List.of();
    logger.info("Retrieved {} buckets", result.size());
    return result;
  }

  private <T> ResponseEntity<T> authenticatedGet(String url, Class<T> responseType) {
    String token = System.getProperty("GARAGE_SECRET_KEY");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    return restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
  }

  /**
   * Lists objects (files) in the given S3-compatible bucket.
   */
  public List<File> listFiles(String bucket) {
    ListObjectsV2Response resp = s3Client.listObjectsV2(ListObjectsV2Request.builder()
        .bucket(bucket)
        .build());
    return resp.contents().stream()
        .map(obj -> {
          File f = new File();
          f.setKey(obj.key());
          f.setSize(obj.size() != null ? obj.size().intValue() : 0);
          if (obj.lastModified() != null)
            f.setLastModified(obj.lastModified().toString());
          return f;
        })
        .collect(Collectors.toList());
  }

  /**
   * Uploads or overwrites a file in the bucket.
   */
  public boolean putFile(String bucket, String key, String content) {
    s3Client.putObject(PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build(),
        software.amazon.awssdk.core.sync.RequestBody.fromString(content));
    return true;
  }

  /**
   * Deletes a file from the bucket.
   */
  public boolean deleteFile(String bucket, String key) {
    s3Client.deleteObject(DeleteObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build());
    return true;
  }

  /**
   * Starts a multipart upload and returns the upload ID.
   */
  public String startMultipartUpload(String bucket, String key) {
    CreateMultipartUploadResponse resp = s3Client.createMultipartUpload(
        CreateMultipartUploadRequest.builder()
            .bucket(bucket)
            .key(key)
            .build());
    return resp.uploadId();
  }

  /**
   * Uploads a single part and returns its ETag.
   */
  public String uploadPart(String bucket, String key, String uploadId, int partNumber, String content) {
    UploadPartResponse resp = s3Client.uploadPart(
        UploadPartRequest.builder()
            .bucket(bucket)
            .key(key)
            .uploadId(uploadId)
            .partNumber(partNumber)
            .build(),
        software.amazon.awssdk.core.sync.RequestBody.fromString(content));
    return resp.eTag();
  }

  /**
   * Completes a multipart upload using the provided parts.
   */
  public boolean completeMultipartUpload(String bucket, String key, String uploadId, List<CompletedPart> parts) {
    List<software.amazon.awssdk.services.s3.model.CompletedPart> completedParts = parts.stream()
        .map(p -> software.amazon.awssdk.services.s3.model.CompletedPart.builder()
            .partNumber(p.getPartNumber())
            .eTag(p.getEtag())
            .build())
        .collect(Collectors.toList());

    s3Client.completeMultipartUpload(
        CompleteMultipartUploadRequest.builder()
            .bucket(bucket)
            .key(key)
            .uploadId(uploadId)
            .multipartUpload(CompletedMultipartUpload.builder()
                .parts(completedParts)
                .build())
            .build());
    return true;
  }
}
