package com.sun.dionysus.transcode;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for TranscodeService command construction.
 */
class TranscodeServiceTest {

  @Test
  void buildCommand_usesFullGpuPathFor4kHevc() {
    TranscodeService service = new TranscodeService(new TranscodeProperties());
    MediaInfo info = new MediaInfo("hevc", 2160, "yuv420p10le", "aac", 120);

    List<String> command = service.buildCommand(Path.of("/in.mkv"), Path.of("/out.mp4"), info, true);

    assertThat(command).containsSequence("-hwaccel", "cuda", "-hwaccel_output_format", "cuda");
    assertThat(command).containsSequence("-vf", "scale_cuda=-2:1080:format=yuv420p");
    assertThat(command).containsSequence("-c:v", "h264_nvenc");
    assertThat(command).containsSequence("-cq", "23");
    assertThat(command).containsSequence("-c:a", "copy");
    assertThat(command).contains("-nostdin");
    assertThat(command).contains("+faststart");
  }

  @Test
  void buildCommand_doesNotUpscaleSmallerHevcSources() {
    TranscodeService service = new TranscodeService(new TranscodeProperties());
    MediaInfo info = new MediaInfo("hevc", 720, "yuv420p", "aac", 120);

    List<String> command = service.buildCommand(Path.of("/in.mkv"), Path.of("/out.mp4"), info, true);

    assertThat(command).containsSequence("-vf", "scale_cuda=format=yuv420p");
    assertThat(command).doesNotContain("scale_cuda=-2:1080:format=yuv420p");
    assertThat(command).containsSequence("-c:v", "h264_nvenc");
  }

  @Test
  void buildCommand_remuxesEightBitH264WithinHeightLimit() {
    TranscodeService service = new TranscodeService(new TranscodeProperties());
    MediaInfo info = new MediaInfo("h264", 1080, "yuv420p", "aac", 120);

    List<String> command = service.buildCommand(Path.of("/in.mkv"), Path.of("/out.mp4"), info, false);

    assertThat(command).containsSequence("-c:v", "copy");
    assertThat(command).doesNotContain("h264_nvenc");
    assertThat(command).containsSequence("-c:a", "copy");
  }

  @Test
  void buildCommand_doesNotRemuxTenBitH264() {
    TranscodeService service = new TranscodeService(new TranscodeProperties());
    MediaInfo info = new MediaInfo("h264", 1080, "yuv420p10le", "aac", 120);

    List<String> command = service.buildCommand(Path.of("/in.mkv"), Path.of("/out.mp4"), info, true);

    assertThat(command).doesNotContainSequence("-c:v", "copy");
    assertThat(command).containsSequence("-c:v", "h264_nvenc");
    assertThat(command).containsSequence("-pix_fmt", "yuv420p");
    assertThat(command).containsSequence("-hwaccel", "cuda");
    assertThat(command).doesNotContain("-hwaccel_output_format");
  }

  @Test
  void buildCommand_reencodes4kH264ToDownscale() {
    TranscodeService service = new TranscodeService(new TranscodeProperties());
    MediaInfo info = new MediaInfo("h264", 2160, "yuv420p", "aac", 120);

    List<String> command = service.buildCommand(Path.of("/in.mkv"), Path.of("/out.mp4"), info, true);

    assertThat(command).containsSequence("-c:v", "h264_nvenc");
    assertThat(command).containsSequence("-vf", "scale=-2:1080");
    assertThat(command).containsSequence("-pix_fmt", "yuv420p");
    assertThat(command).doesNotContain("-hwaccel_output_format");
  }

  @Test
  void buildCommand_transcodesIncompatibleAudio() {
    TranscodeService service = new TranscodeService(new TranscodeProperties());
    MediaInfo info = new MediaInfo("hevc", 1080, "yuv420p", "dts", 120);

    List<String> command = service.buildCommand(Path.of("/in.mkv"), Path.of("/out.mp4"), info, true);

    assertThat(command).containsSequence("-c:a", "aac", "-b:a", "384k");
  }

  @Test
  void buildCommand_usesX264WhenNvencIsDisabled() {
    TranscodeService service = new TranscodeService(new TranscodeProperties());
    MediaInfo info = new MediaInfo("hevc", 2160, "yuv420p10le", "aac", 120);

    List<String> command = service.buildCommand(Path.of("/in.mkv"), Path.of("/out.mp4"), info, false);

    assertThat(command).containsSequence("-c:v", "libx264");
    assertThat(command).containsSequence("-vf", "scale=-2:1080");
    assertThat(command).containsSequence("-pix_fmt", "yuv420p");
    assertThat(command).doesNotContain("cuda");
  }

  @Test
  void canCopyAudio_acceptsMp4CompatibleCodecs() {
    TranscodeService service = new TranscodeService(new TranscodeProperties());

    assertThat(service.canCopyAudio("AAC")).isTrue();
    assertThat(service.canCopyAudio("eac3")).isTrue();
    assertThat(service.canCopyAudio("dts")).isFalse();
    assertThat(service.canCopyAudio(null)).isFalse();
  }

  @Test
  void needsDownscale_respectsDisabledLimit() {
    TranscodeProperties properties = new TranscodeProperties();
    properties.setMaxHeight(0);
    TranscodeService service = new TranscodeService(properties);

    assertThat(service.needsDownscale(new MediaInfo("hevc", 2160, "yuv420p10le", "aac", 120)))
        .isFalse();
  }
}
