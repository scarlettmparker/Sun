package com.sun.dionysus.torrent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.dionysus.model.TorrentJobEntity;
import com.sun.dionysus.model.enums.TorrentStatus;
import com.sun.dionysus.service.KeyDetailService;
import com.sun.dionysus.service.torrent.TorrentJobService;
import com.sun.dionysus.transcode.TranscodeProperties;
import com.sun.dionysus.transcode.TranscodeResult;
import com.sun.dionysus.transcode.TranscodeService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Unit tests for TorrentCompletionService transcoding.
 */
@ExtendWith(MockitoExtension.class)
class TorrentCompletionServiceTest {

  @TempDir Path tempDir;

  @Mock private TorrentJobService jobService;
  @Mock private KeyDetailService keyDetailService;
  @Mock private S3Client s3Client;
  @Mock private S3Presigner s3Presigner;
  @Mock private TorrentJobRegistry registry;
  @Mock private TransmissionGateway transmissionGateway;
  @Mock private TorrentClientProperties properties;
  @Mock private TranscodeService transcodeService;
  @Mock private TranscodeProperties transcodeProperties;
  @Spy @InjectMocks private TorrentCompletionService service;

  @Test
  void complete_keepsOriginalWhenConfigured() throws Exception {
    Path root = Files.createDirectory(tempDir.resolve("root"));
    Path scratch = Files.createDirectory(root.resolve("job"));
    Path video = Files.writeString(scratch.resolve("movie.mkv"), "video");
    Path mp4 = Files.writeString(tempDir.resolve("out.mp4"), "mp4");
    TorrentJobEntity job = newJob(scratch);
    UUID jobId = job.getId();

    when(jobService.findById(jobId)).thenReturn(Optional.of(job));
    when(jobService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(properties.getScratchDir()).thenReturn(root.toString());
    when(transcodeService.isVideoFile(any())).thenReturn(true);
    when(transcodeService.transcode(any(), any()))
        .thenReturn(new TranscodeResult(mp4, false, "h264_nvenc"));
    when(transcodeProperties.isKeepOriginal()).thenReturn(true);
    doReturn(new ArrayList<>(List.of("tv/movie/movie.mkv")))
        .when(service).upload(any(), any());
    doNothing().when(service).putFile(anyString(), anyString(), any());

    service.complete(jobId);

    assertThat(job.getStatus()).isEqualTo(TorrentStatus.COMPLETED);
    assertThat(job.getProgress()).isEqualTo(1.0);
    verify(service).putFile("bucket", "tv/movie/movie.mkv.mp4", mp4);
    verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    verify(keyDetailService)
        .createOrUpdateDetail(eq("bucket"), eq("tv/movie/movie.mkv"), eq("movie.mkv"), anyString());
    verify(keyDetailService)
        .createOrUpdateDetail(
            eq("bucket"), eq("tv/movie/movie.mkv.mp4"), eq("movie.mkv.mp4"), anyString());
    verify(transmissionGateway).removeTransmission(jobId, true);
  }

  @Test
  void complete_deletesOriginalWhenConfigured() throws Exception {
    Path root = Files.createDirectory(tempDir.resolve("root"));
    Path scratch = Files.createDirectory(root.resolve("job"));
    Path video = Files.writeString(scratch.resolve("movie.mkv"), "video");
    Path mp4 = Files.writeString(tempDir.resolve("out.mp4"), "mp4");
    TorrentJobEntity job = newJob(scratch);
    UUID jobId = job.getId();

    when(jobService.findById(jobId)).thenReturn(Optional.of(job));
    when(jobService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(properties.getScratchDir()).thenReturn(root.toString());
    when(transcodeService.isVideoFile(any())).thenReturn(true);
    when(transcodeService.transcode(any(), any()))
        .thenReturn(new TranscodeResult(mp4, false, "h264_nvenc"));
    when(transcodeProperties.isKeepOriginal()).thenReturn(false);
    when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
        .thenReturn(DeleteObjectResponse.builder().build());
    doReturn(new ArrayList<>(List.of("tv/movie/movie.mkv")))
        .when(service).upload(any(), any());
    doNothing().when(service).putFile(anyString(), anyString(), any());

    service.complete(jobId);

    ArgumentCaptor<DeleteObjectRequest> deleted = ArgumentCaptor.forClass(DeleteObjectRequest.class);
    verify(s3Client).deleteObject(deleted.capture());
    assertThat(deleted.getValue().key()).isEqualTo("tv/movie/movie.mkv");
    verify(keyDetailService, never())
        .createOrUpdateDetail(
            eq("bucket"), eq("tv/movie/movie.mkv"), eq("movie.mkv"), anyString());
    verify(keyDetailService)
        .createOrUpdateDetail(
            eq("bucket"), eq("tv/movie/movie.mkv.mp4"), eq("movie.mkv.mp4"), anyString());
  }

  @Test
  void complete_keepsOriginalWhenTranscodeFails() throws Exception {
    Path root = Files.createDirectory(tempDir.resolve("root"));
    Path scratch = Files.createDirectory(root.resolve("job"));
    Files.writeString(scratch.resolve("movie.mkv"), "video");
    TorrentJobEntity job = newJob(scratch);
    UUID jobId = job.getId();

    when(jobService.findById(jobId)).thenReturn(Optional.of(job));
    when(jobService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(properties.getScratchDir()).thenReturn(root.toString());
    when(transcodeService.isVideoFile(any())).thenReturn(true);
    when(transcodeService.transcode(any(), any())).thenThrow(new IOException("boom"));
    doReturn(new ArrayList<>(List.of("tv/movie/movie.mkv")))
        .when(service).upload(any(), any());

    service.complete(jobId);

    assertThat(job.getStatus()).isEqualTo(TorrentStatus.COMPLETED);
    verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    verify(keyDetailService)
        .createOrUpdateDetail(eq("bucket"), eq("tv/movie/movie.mkv"), eq("movie.mkv"), anyString());
    verify(keyDetailService, never())
        .createOrUpdateDetail(
            eq("bucket"), eq("tv/movie/movie.mkv.mp4"), eq("movie.mkv.mp4"), anyString());
  }

  @Test
  void complete_reportsTranscodeProgress() throws Exception {
    Path root = Files.createDirectory(tempDir.resolve("root"));
    Path scratch = Files.createDirectory(root.resolve("job"));
    Files.writeString(scratch.resolve("movie.mkv"), "video");
    Path mp4 = Files.writeString(tempDir.resolve("out.mp4"), "mp4");
    TorrentJobEntity job = newJob(scratch);
    UUID jobId = job.getId();
    AtomicReference<Double> reportedProgress = new AtomicReference<>();

    when(jobService.findById(jobId)).thenReturn(Optional.of(job));
    when(jobService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(properties.getScratchDir()).thenReturn(root.toString());
    when(transcodeService.isVideoFile(any())).thenReturn(true);
    when(transcodeService.transcode(any(), any())).thenAnswer(invocation -> {
      @SuppressWarnings("unchecked")
      Consumer<Double> listener = invocation.getArgument(1);
      listener.accept(0.42);
      reportedProgress.set(job.getProgress());
      return new TranscodeResult(mp4, false, "h264_nvenc");
    });
    when(transcodeProperties.isKeepOriginal()).thenReturn(true);
    doReturn(new ArrayList<>(List.of("tv/movie/movie.mkv")))
        .when(service).upload(any(), any());
    doNothing().when(service).putFile(anyString(), anyString(), any());

    service.complete(jobId);

    assertThat(reportedProgress.get()).isEqualTo(0.42);
  }

  /**
   * Creates a downloadable job for a scratch directory.
   *
   * @param scratch the scratch directory.
   * @return the torrent job.
   */
  private TorrentJobEntity newJob(Path scratch) {
    TorrentJobEntity job = new TorrentJobEntity();
    job.setId(UUID.randomUUID());
    job.setStatus(TorrentStatus.DOWNLOADING);
    job.setBucket("bucket");
    job.setTargetKeyPath("tv/movie");
    job.setScratchPath(scratch.toString());
    return job;
  }
}
