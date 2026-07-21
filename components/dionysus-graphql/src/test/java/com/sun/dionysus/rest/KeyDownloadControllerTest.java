package com.sun.dionysus.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link com.sun.dionysus.rest.KeyDownloadController}.
 *
 * <p>
 * Verifies download behavior for valid files, directories, missing keys, and
 * not-found keys.
 */
@WebMvcTest(KeyDownloadController.class)
public class KeyDownloadControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private S3Client s3Client;

  /**
   * Confirms that a non-directory key returns the expected file stream and
   * headers.
   */
  @Test
  void downloadKey_returnsFileStream() throws Exception {
    byte[] payload = "hello".getBytes(StandardCharsets.UTF_8);

    when(s3Client.headObject(any(HeadObjectRequest.class)))
        .thenReturn(HeadObjectResponse.builder().contentLength((long) payload.length).build());

    ResponseInputStream<GetObjectResponse> objectStream = new ResponseInputStream<>(
        GetObjectResponse.builder().contentLength((long) payload.length).build(),
        AbortableInputStream.create(new ByteArrayInputStream(payload)));

    when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(objectStream);

    mockMvc.perform(get("/api/buckets/test-bucket/download").param("key", "folder/file.txt"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", containsString("file.txt")))
        .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE))
        .andExpect(content().bytes(payload));
  }

  /**
   * Verifies that directory-style keys are rejected with bad request.
   */
  @Test
  void downloadKey_directoryKey_returnsBadRequest() throws Exception {
    mockMvc.perform(get("/api/buckets/test-bucket/download").param("key", "folder/"))
        .andExpect(status().isBadRequest());
  }

  /**
   * Verifies that requests missing the key query parameter return bad request.
   */
  @Test
  void downloadKey_missingKey_returnsBadRequest() throws Exception {
    mockMvc.perform(get("/api/buckets/test-bucket/download"))
        .andExpect(status().isBadRequest());
  }

  /**
   * Verifies that a missing object on the S3 client results in a 404 response.
   */
  @Test
  void downloadKey_nonexistentKey_returnsNotFound() throws Exception {
    when(s3Client.headObject(any(HeadObjectRequest.class)))
        .thenThrow(NoSuchKeyException.builder().message("Not found").build());

    mockMvc.perform(get("/api/buckets/test-bucket/download").param("key", "missing.txt"))
        .andExpect(status().isNotFound());
  }
}
