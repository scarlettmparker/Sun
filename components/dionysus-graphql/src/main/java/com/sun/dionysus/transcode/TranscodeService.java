package com.sun.dionysus.transcode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

/**
 * Runs browser-friendly video transcodes with optional GPU acceleration.
 */
@Service
@EnableConfigurationProperties(TranscodeProperties.class)
public class TranscodeService {

  private static final Logger logger = LoggerFactory.getLogger(TranscodeService.class);
  private static final Set<String> MP4_AUDIO_CODECS = Set.of("aac", "mp3", "mp2", "ac3", "eac3");
  private static final Set<String> REMUX_PIXEL_FORMATS = Set.of("yuv420p", "yuvj420p", "nv12");
  private static final Set<String> NVDEC_CODECS = Set.of(
      "h264", "hevc", "av1", "vp9", "vp8", "mpeg2video", "vc1");
  private static final String NVENC_ENCODER = "h264_nvenc";
  private static final String X264_ENCODER = "libx264";
  private static final int AUDIO_BITRATE_KBPS = 384;

  private final TranscodeProperties properties;
  private final Semaphore permits;
  private volatile Boolean nvencAvailable;

  /**
   * Creates a transcode service.
   *
   * @param properties the transcode settings.
   */
  public TranscodeService(TranscodeProperties properties) {
    this.properties = properties;
    this.permits = new Semaphore(Math.max(1, properties.getMaxConcurrent()), true);
  }

  /**
   * Transcodes a video into a browser-friendly MP4.
   *
   * @param input the source video.
   * @param progressListener receives progress values between zero and one, or null.
   * @return the completed transcode.
   * @throws IOException when probing, encoding, or writing fails.
   * @throws InterruptedException when the thread is interrupted.
   */
  public TranscodeResult transcode(Path input, Consumer<Double> progressListener)
      throws IOException, InterruptedException {
    MediaInfo info = probe(input);
    boolean remux = canRemux(info);
    boolean useNvenc = !remux && nvencAvailable();

    permits.acquire();
    Path output = null;
    try {
      output = createOutputFile();
      int exit = runFfmpeg(buildCommand(input, output, info, useNvenc), info.durationSeconds(), progressListener);
      if (exit != 0 && useNvenc) {
        logger.warn("NVENC failed for {} with code {}; retrying with x264", input, exit);
        useNvenc = false;
        exit = runFfmpeg(buildCommand(input, output, info, false), info.durationSeconds(), progressListener);
      }
      if (exit != 0) {
        throw new IOException("ffmpeg exited with code " + exit);
      }
      String encoder = remux ? "copy" : (useNvenc ? NVENC_ENCODER : X264_ENCODER);
      return new TranscodeResult(output, remux, encoder);
    } catch (IOException | InterruptedException | RuntimeException e) {
      deleteQuietly(output);
      throw e;
    } finally {
      permits.release();
    }
  }

  /**
   * Reports whether a file is an MKV or AVI video.
   *
   * @param file the candidate file.
   * @return true for MKV and AVI files.
   */
  public boolean isVideoFile(Path file) {
    String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
    if (name.endsWith(".mkv") || name.endsWith(".avi")) {
      return true;
    }
    try {
      String mime = Files.probeContentType(file);
      return "video/x-matroska".equals(mime)
          || "application/x-matroska".equals(mime)
          || "video/avi".equals(mime);
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Builds the ffmpeg command for a transcode.
   *
   * @param input the source video.
   * @param output the destination MP4.
   * @param info the probed source metadata.
   * @param useNvenc whether the GPU encoder should be used.
   * @return the ffmpeg command.
   */
  List<String> buildCommand(Path input, Path output, MediaInfo info, boolean useNvenc) {
    boolean remux = canRemux(info);
    List<String> command = new ArrayList<>();
    command.add("ffmpeg");
    command.add("-hide_banner");
    command.add("-nostdin");
    command.add("-y");
    boolean fullHwDecode = useNvenc && !remux && supportsFullHwDecode(info);
    boolean hardwareDecode = useNvenc && !remux && supportsNvdec(info);
    if (fullHwDecode) {
      command.add("-hwaccel");
      command.add("cuda");
      command.add("-hwaccel_output_format");
      command.add("cuda");
    } else if (hardwareDecode) {
      command.add("-hwaccel");
      command.add("cuda");
    }
    command.add("-i");
    command.add(input.toString());
    command.add("-map");
    command.add("0:v:0");
    if (info.audioCodec() != null) {
      command.add("-map");
      command.add("0:a:0?");
    }
    if (remux) {
      command.add("-c:v");
      command.add("copy");
    } else if (useNvenc) {
      if (fullHwDecode) {
        command.add("-vf");
        command.add(nvencFilter(info));
      } else if (needsDownscale(info)) {
        command.add("-vf");
        command.add("scale=-2:" + properties.getMaxHeight());
      }
      command.add("-c:v");
      command.add(NVENC_ENCODER);
      command.add("-preset");
      command.add(properties.getPreset());
      command.add("-tune");
      command.add("hq");
      command.add("-rc");
      command.add("vbr");
      command.add("-cq");
      command.add(String.valueOf(properties.getCq()));
      command.add("-b:v");
      command.add("0");
      if (!fullHwDecode) {
        command.add("-pix_fmt");
        command.add("yuv420p");
      }
    } else {
      if (needsDownscale(info)) {
        command.add("-vf");
        command.add("scale=-2:" + properties.getMaxHeight());
      }
      command.add("-c:v");
      command.add(X264_ENCODER);
      command.add("-preset");
      command.add(properties.getX264Preset());
      command.add("-crf");
      command.add(String.valueOf(properties.getCq()));
      command.add("-pix_fmt");
      command.add("yuv420p");
    }
    if (info.audioCodec() != null) {
      if (canCopyAudio(info.audioCodec())) {
        command.add("-c:a");
        command.add("copy");
      } else {
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add(AUDIO_BITRATE_KBPS + "k");
      }
    }
    command.add("-movflags");
    command.add("+faststart");
    command.add("-progress");
    command.add("pipe:1");
    command.add("-nostats");
    command.add(output.toString());
    return command;
  }

  /**
   * Reports whether the source can be copied into MP4 without re-encoding.
   *
   * @param info the probed source metadata.
   * @return true when the video is H.264 and the height is within the limit.
   */
  boolean canRemux(MediaInfo info) {
    String pixelFormat = info.pixelFormat() == null
        ? ""
        : info.pixelFormat().toLowerCase(Locale.ROOT);
    return "h264".equalsIgnoreCase(info.videoCodec())
        && REMUX_PIXEL_FORMATS.contains(pixelFormat)
        && !needsDownscale(info);
  }

  /**
   * Reports whether the source must be scaled down.
   *
   * @param info the probed source metadata.
   * @return true when the configured maximum height is exceeded.
   */
  boolean needsDownscale(MediaInfo info) {
    return properties.getMaxHeight() > 0 && info.height() > properties.getMaxHeight();
  }

  /**
   * Builds the CUDA scaling filter.
   *
   * @param info the probed source metadata.
   * @return the filter expression.
   */
  String nvencFilter(MediaInfo info) {
    if (needsDownscale(info)) {
      return "scale_cuda=-2:" + properties.getMaxHeight() + ":format=yuv420p";
    }
    return "scale_cuda=format=yuv420p";
  }

  /**
   * Reports whether an audio codec can be copied into MP4.
   *
   * @param codec the source audio codec.
   * @return true when the codec is MP4 compatible.
   */
  boolean canCopyAudio(String codec) {
    return codec != null && MP4_AUDIO_CODECS.contains(codec.toLowerCase(Locale.ROOT));
  }

  /**
   * Probes video, audio, and duration metadata.
   *
   * @param input the source video.
   * @return the probed metadata.
   * @throws IOException when probing fails.
   * @throws InterruptedException when the thread is interrupted.
   */
  MediaInfo probe(Path input) throws IOException, InterruptedException {
    String video = probeValue(input, "v:0", "stream=codec_name,height,pix_fmt");
    String[] videoParts = video.split(",");
    if (videoParts.length < 2) {
      throw new IOException("Could not probe video metadata for " + input);
    }
    int height;
    try {
      height = Integer.parseInt(videoParts[1].trim());
    } catch (NumberFormatException e) {
      throw new IOException("Could not read video height for " + input, e);
    }
    String pixelFormat = videoParts.length > 2 ? videoParts[2].trim() : "";
    String audio = probeValue(input, "a:0", "stream=codec_name");
    String audioCodec = audio.isBlank() ? null : audio.trim();
    return new MediaInfo(
        videoParts[0].trim(), height, pixelFormat, audioCodec, probeDuration(input));
  }

  /**
   * Reports whether the source can be decoded fully on the GPU.
   *
   * @param info the probed source metadata.
   * @return true when the source uses HEVC.
   */
  boolean supportsFullHwDecode(MediaInfo info) {
    return "hevc".equalsIgnoreCase(info.videoCodec());
  }

  /**
   * Reports whether NVDEC supports the source codec.
   *
   * @param info the probed source metadata.
   * @return true when the codec has an NVDEC decoder.
   */
  boolean supportsNvdec(MediaInfo info) {
    return info.videoCodec() != null
        && NVDEC_CODECS.contains(info.videoCodec().toLowerCase(Locale.ROOT));
  }

  /**
   * Reports whether the NVENC encoder is usable.
   *
   * @return true when hardware encoding is available.
   */
  boolean nvencAvailable() {
    if (nvencAvailable != null) {
      return nvencAvailable;
    }
    synchronized (this) {
      if (nvencAvailable != null) {
        return nvencAvailable;
      }
      String configured = properties.getEncoder() == null
          ? "auto"
          : properties.getEncoder().toLowerCase(Locale.ROOT);
      if ("x264".equals(configured)) {
        nvencAvailable = false;
        return false;
      }
      boolean available = testNvenc();
      nvencAvailable = available;
      if (available) {
        logger.info("Transcode encoder: {}", NVENC_ENCODER);
      } else {
        logger.warn("NVENC is unavailable; using {}", X264_ENCODER);
      }
      return available;
    }
  }

  /**
   * Runs one ffmpeg process and reports progress.
   *
   * @param command the command to run.
   * @param durationSeconds the source duration in seconds.
   * @param progressListener receives progress values, or null.
   * @return the process exit code.
   * @throws IOException when the process cannot start.
   * @throws InterruptedException when the thread is interrupted.
   */
  private int runFfmpeg(List<String> command, double durationSeconds, Consumer<Double> progressListener)
      throws IOException, InterruptedException {
    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    double lastReported = -1;
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (progressListener == null) {
          continue;
        }
        double progress = parseProgress(line, durationSeconds);
        if (progress >= 0 && (progress - lastReported >= 0.01 || progress >= 1.0)) {
          progressListener.accept(progress);
          lastReported = progress;
        }
      }
    } finally {
      if (process.isAlive()) {
        process.destroyForcibly();
      }
    }
    int exit = process.waitFor();
    if (exit == 0 && progressListener != null) {
      progressListener.accept(1.0);
    }
    return exit;
  }

  /**
   * Parses one ffmpeg progress line.
   *
   * @param line the progress line.
   * @param durationSeconds the source duration in seconds.
   * @return the progress between zero and one, or minus one when the line is not progress.
   */
  private double parseProgress(String line, double durationSeconds) {
    if (durationSeconds <= 0) {
      return -1;
    }
    if (!line.startsWith("out_time_us=") && !line.startsWith("out_time_ms=")) {
      return -1;
    }
    String value = line.substring(line.indexOf('=') + 1).trim();
    try {
      long microseconds = Long.parseLong(value);
      return Math.min(microseconds / 1_000_000.0 / durationSeconds, 1.0);
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  /**
   * Tests whether a one-frame NVENC encode succeeds.
   *
   * @return true when hardware encoding is available.
   */
  private boolean testNvenc() {
    List<String> command = List.of(
        "ffmpeg", "-hide_banner", "-loglevel", "error", "-nostdin", "-y",
        "-f", "lavfi", "-i", "color=size=256x256:rate=1:duration=1",
        "-c:v", NVENC_ENCODER, "-f", "null", "-");
    try {
      Process process = new ProcessBuilder(command)
          .redirectErrorStream(true)
          .redirectOutput(ProcessBuilder.Redirect.DISCARD)
          .start();
      if (!process.waitFor(10, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        return false;
      }
      return process.exitValue() == 0;
    } catch (IOException e) {
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  /**
   * Reads one ffprobe field set.
   *
   * @param input the source video.
   * @param stream the stream selector.
   * @param entries the ffprobe entries expression.
   * @return the trimmed output.
   * @throws IOException when probing fails.
   * @throws InterruptedException when the thread is interrupted.
   */
  private String probeValue(Path input, String stream, String entries)
      throws IOException, InterruptedException {
    List<String> command = List.of(
        "ffprobe", "-v", "error", "-select_streams", stream,
        "-show_entries", entries, "-of", "csv=p=0", input.toString());
    return runProbe(command).trim();
  }

  /**
   * Reads the container duration.
   *
   * @param input the source video.
   * @return the duration in seconds, or zero when it cannot be read.
   * @throws IOException when probing fails.
   * @throws InterruptedException when the thread is interrupted.
   */
  private double probeDuration(Path input) throws IOException, InterruptedException {
    List<String> command = List.of(
        "ffprobe", "-v", "error",
        "-show_entries", "format=duration",
        "-of", "default=nw=1:nk=1", input.toString());
    try {
      return Double.parseDouble(runProbe(command).trim());
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  /**
   * Runs an ffprobe command.
   *
   * @param command the command to run.
   * @return the standard output.
   * @throws IOException when the process cannot start or fails.
   * @throws InterruptedException when the thread is interrupted.
   */
  private String runProbe(List<String> command) throws IOException, InterruptedException {
    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    String output;
    try (var stream = process.getInputStream()) {
      output = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
    int exit = process.waitFor();
    if (exit != 0) {
      throw new IOException("ffprobe exited with code " + exit + ": " + output.trim());
    }
    return output;
  }

  /**
   * Creates a temporary output file in the configured directory.
   *
   * @return the temporary MP4 file.
   * @throws IOException when the directory or file cannot be created.
   */
  private Path createOutputFile() throws IOException {
    Path directory = Path.of(properties.getTempDir());
    Files.createDirectories(directory);
    return Files.createTempFile(directory, "transcode-", ".mp4");
  }

  /**
   * Deletes a file without failing the caller.
   *
   * @param path the file to delete, or null.
   */
  private void deleteQuietly(Path path) {
    if (path == null) {
      return;
    }
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      logger.warn("Could not delete temporary transcode file {}", path, e);
    }
  }
}
