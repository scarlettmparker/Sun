package com.sun.dionysus.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sun.dionysus.transcode.TranscodeResult;
import com.sun.dionysus.transcode.TranscodeService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Unit tests for FileViewController transcoding.
 */
@ExtendWith(MockitoExtension.class)
class FileViewControllerTest {

  @TempDir Path tempDir;

  @Mock private S3Client s3Client;
  @Mock private S3Presigner s3Presigner;
  @Mock private TranscodeService transcodeService;
  @Spy @InjectMocks private FileViewController controller;

  @Test
  void transcodeFile_delegatesToServiceAndUploadsMp4() throws Exception {
    Path input = Files.createTempFile(tempDir, "in-", ".mkv");
    Path mp4 = Files.writeString(tempDir.resolve("out.mp4"), "mp4");
    when(transcodeService.createWorkFile(anyString(), anyString())).thenReturn(input);
    when(s3Client.headObject(any(HeadObjectRequest.class)))
        .thenReturn(HeadObjectResponse.builder()
            .contentLength(5L)
            .contentType("video/x-matroska")
            .build());
    when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(stream("video"));
    when(transcodeService.transcode(any(Path.class), isNull()))
        .thenReturn(new TranscodeResult(mp4, false, "h264_nvenc"));
    doNothing().when(controller).uploadViaPresign("bucket", "movie.mkv.mp4", mp4);

    ResponseEntity<String> response = controller.transcodeFile("bucket", "movie.mkv");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo("movie.mkv.mp4");
    verify(transcodeService).transcode(any(Path.class), isNull());
    verify(controller).uploadViaPresign("bucket", "movie.mkv.mp4", mp4);
  }

  @Test
  void transcodeFile_rejectsNonVideoKeys() {
    ResponseEntity<String> response = controller.transcodeFile("bucket", "movie.txt");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    verifyNoInteractions(transcodeService);
  }

  @Test
  void transcodeFile_returnsNotFoundForMissingObject() {
    when(s3Client.headObject(any(HeadObjectRequest.class)))
        .thenThrow(NoSuchKeyException.builder().message("missing").build());

    ResponseEntity<String> response = controller.transcodeFile("bucket", "movie.mkv");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    verifyNoInteractions(transcodeService);
  }

  @Test
  void transcodeFile_returnsServerErrorWhenServiceFails() throws Exception {
    Path input = Files.createTempFile(tempDir, "in-", ".mkv");
    when(transcodeService.createWorkFile(anyString(), anyString())).thenReturn(input);
    when(s3Client.headObject(any(HeadObjectRequest.class)))
        .thenReturn(HeadObjectResponse.builder()
            .contentLength(5L)
            .contentType("video/x-matroska")
            .build());
    when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(stream("video"));
    when(transcodeService.transcode(any(Path.class), isNull()))
        .thenThrow(new IOException("boom"));

    ResponseEntity<String> response = controller.transcodeFile("bucket", "movie.mkv");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * Creates an S3 object stream for a string payload.
   *
   * @param value the payload.
   * @return the object stream.
   */
  private ResponseInputStream<GetObjectResponse> stream(String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    return new ResponseInputStream<>(
        GetObjectResponse.builder().contentLength((long) bytes.length).build(),
        AbortableInputStream.create(new ByteArrayInputStream(bytes)));
  }
}
