package com.sun.dionysus.subtitle;

import com.sun.dionysus.transcode.TranscodeProperties;
import com.sun.dionysus.transcode.TranscodeService;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Converts subtitle formats to WebVTT.
 */
public class SrtToVtt {

  private final TranscodeService transcodeService;

  /**
   * Creates a converter with a default ffmpeg service.
   */
  public SrtToVtt() {
    this(new TranscodeService(new TranscodeProperties()));
  }

  /**
   * Creates a converter.
   *
   * @param transcodeService the ffmpeg service, or null for a default
   */
  public SrtToVtt(TranscodeService transcodeService) {
    this.transcodeService = transcodeService == null
        ? new TranscodeService(new TranscodeProperties())
        : transcodeService;
  }

  /**
   * Converts a subtitle file to WebVTT.
   *
   * @param source the subtitle file
   * @return the WebVTT text
   * @throws IOException when conversion fails
   */
  public static String convert(Path source) throws IOException {
    return new SrtToVtt().convertFile(source);
  }

  /**
   * Converts subtitle bytes to WebVTT.
   *
   * @param data the subtitle bytes
   * @return the WebVTT text
   * @throws IOException when conversion fails
   */
  public static String convert(byte[] data) throws IOException {
    return convert(data, null);
  }

  /**
   * Converts subtitle bytes to WebVTT.
   *
   * @param data the subtitle bytes
   * @param fileName the source file name, or null
   * @return the WebVTT text
   * @throws IOException when conversion fails
   */
  public static String convert(byte[] data, String fileName) throws IOException {
    return new SrtToVtt().convertBytes(data, fileName);
  }

  /**
   * Converts SRT text to WebVTT.
   *
   * @param text the subtitle text
   * @return the WebVTT text
   */
  public static String convert(String text) {
    if (text == null) {
      return "WEBVTT\n";
    }
    String trimmed = text.trim();
    if (trimmed.startsWith("WEBVTT")) {
      return text;
    }
    if (trimmed.contains("[Script Info]") || trimmed.toLowerCase(Locale.ROOT).contains("dialogue:")) {
      // ASS/SSA text cannot be converted without a file; treat as plain text passthrough.
      return text;
    }
    return convertSrt(text);
  }

  /**
   * Converts a subtitle file to WebVTT.
   *
   * @param source the subtitle file
   * @return the WebVTT text
   * @throws IOException when conversion fails
   */
  public String convertFile(Path source) throws IOException {
    if (source == null) {
      throw new IllegalArgumentException("source must not be null");
    }
    byte[] data = Files.readAllBytes(source);
    String name = source.getFileName() == null ? "" : source.getFileName().toString();
    return convertBytes(data, name);
  }

  /**
   * Converts subtitle bytes to WebVTT.
   *
   * @param data the subtitle bytes
   * @param fileName the source file name, or null
   * @return the WebVTT text
   * @throws IOException when conversion fails
   */
  public String convertBytes(byte[] data, String fileName) throws IOException {
    if (data == null || data.length == 0) {
      return "WEBVTT\n";
    }

    String extension = extensionOf(fileName);
    String text = decode(data);

    if (extension.equals("vtt") || text.trim().startsWith("WEBVTT")) {
      return text;
    }

    if (extension.equals("ass") || extension.equals("ssa")
        || text.contains("[Script Info]")) {
      return convertAss(data);
    }

    return convertSrt(text);
  }

  /**
   * Converts SRT text to WebVTT.
   *
   * @param text the SRT text
   * @return the WebVTT text
   */
  public static String convertSrt(String text) {
    if (text == null) {
      return "WEBVTT\n";
    }
    String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
    if (normalized.startsWith("\uFEFF")) {
      normalized = normalized.substring(1);
    }
    if (normalized.trim().startsWith("WEBVTT")) {
      return normalized;
    }

    StringBuilder output = new StringBuilder("WEBVTT\n\n");
    String[] blocks = normalized.split("\\n\\s*\\n");
    for (String block : blocks) {
      String[] lines = block.split("\\n");
      int timestampIndex = -1;
      for (int i = 0; i < lines.length; i++) {
        if (lines[i].contains("-->")) {
          timestampIndex = i;
          break;
        }
      }
      if (timestampIndex < 0) {
        continue;
      }
      String timestamp = lines[timestampIndex].replace(',', '.').trim();
      output.append(timestamp).append("\n");
      for (int i = timestampIndex + 1; i < lines.length; i++) {
        output.append(lines[i]).append("\n");
      }
      output.append("\n");
    }
    return output.toString();
  }

  /**
   * Converts ASS/SSA bytes to WebVTT using ffmpeg.
   *
   * @param data the ASS/SSA bytes
   * @return the WebVTT text
   * @throws IOException when conversion fails
   */
  private String convertAss(byte[] data) throws IOException {
    Path input = transcodeService.createWorkFile("subtitle-", ".ass");
    Path output = transcodeService.createWorkFile("subtitle-", ".vtt");
    try {
      Files.write(input, data);
      ProcessBuilder builder = new ProcessBuilder(
          "ffmpeg", "-hide_banner", "-nostdin", "-y", "-i", input.toString(), output.toString());
      builder.redirectErrorStream(true);
      Process process = builder.start();
      String processOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      int exit = process.waitFor();
      if (exit != 0) {
        throw new IOException("ffmpeg exited with code " + exit + ": " + processOutput);
      }
      return Files.readString(output, StandardCharsets.UTF_8);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("ffmpeg interrupted", e);
    } finally {
      deleteQuietly(input);
      deleteQuietly(output);
    }
  }

  /**
   * Decodes subtitle bytes.
   */
  private static String decode(byte[] data) {
    if (data.length >= 3 && (data[0] & 0xff) == 0xef
        && (data[1] & 0xff) == 0xbb && (data[2] & 0xff) == 0xbf) {
      return new String(data, 3, data.length - 3, StandardCharsets.UTF_8);
    }
    if (data.length >= 2 && (data[0] & 0xff) == 0xff && (data[1] & 0xff) == 0xfe) {
      return new String(data, 2, data.length - 2, Charset.forName("UTF-16LE"));
    }
    if (data.length >= 2 && (data[0] & 0xff) == 0xfe && (data[1] & 0xff) == 0xff) {
      return new String(data, 2, data.length - 2, Charset.forName("UTF-16BE"));
    }
    try {
      java.nio.charset.CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
          .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
          .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT);
      return decoder.decode(java.nio.ByteBuffer.wrap(data)).toString();
    } catch (java.nio.charset.CharacterCodingException e) {
      return new String(data, Charset.forName("windows-1252"));
    }
  }

  /**
   * Returns a file extension.
   */
  private static String extensionOf(String fileName) {
    if (fileName == null) {
      return "";
    }
    int dot = fileName.lastIndexOf('.');
    if (dot < 0 || dot == fileName.length() - 1) {
      return "";
    }
    return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
  }

  /**
   * Deletes a file quietly.
   */
  private static void deleteQuietly(Path path) {
    try {
      if (path != null) {
        Files.deleteIfExists(path);
      }
    } catch (IOException ignored) {
    }
  }
}
