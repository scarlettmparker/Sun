package com.sun.dionysus.transcode;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunable settings for browser-friendly video transcoding.
 */
@ConfigurationProperties(prefix = "dionysus.transcode")
public class TranscodeProperties {

  /**
   * Encoder selection: auto, nvenc, or x264.
   */
  private String encoder = "auto";

  /**
   * Maximum output height in pixels, or zero to keep the source height.
   */
  private int maxHeight = 1080;

  /**
   * Constant quality target passed to the encoder.
   */
  private int cq = 23;

  /**
   * Preset used by the NVENC encoder.
   */
  private String preset = "p5";

  /**
   * Preset used by the x264 fallback encoder.
   */
  private String x264Preset = "veryfast";

  /**
   * Directory holding temporary transcode output files.
   */
  private String tempDir = "/var/tmp/dionysus/transcode";

  /**
   * Whether the original video is retained after a successful transcode.
   */
  private boolean keepOriginal = true;

  /**
   * Maximum number of concurrent transcode processes.
   */
  private int maxConcurrent = 1;

  /**
   * Returns the encoder selection.
   *
   * @return the encoder selection.
   */
  public String getEncoder() {
    return encoder;
  }

  /**
   * Sets the encoder selection.
   *
   * @param encoder the encoder selection.
   */
  public void setEncoder(String encoder) {
    this.encoder = encoder;
  }

  /**
   * Returns the maximum output height.
   *
   * @return the maximum output height in pixels.
   */
  public int getMaxHeight() {
    return maxHeight;
  }

  /**
   * Sets the maximum output height.
   *
   * @param maxHeight the maximum output height in pixels.
   */
  public void setMaxHeight(int maxHeight) {
    this.maxHeight = maxHeight;
  }

  /**
   * Returns the constant quality target.
   *
   * @return the constant quality target.
   */
  public int getCq() {
    return cq;
  }

  /**
   * Sets the constant quality target.
   *
   * @param cq the constant quality target.
   */
  public void setCq(int cq) {
    this.cq = cq;
  }

  /**
   * Returns the NVENC preset.
   *
   * @return the NVENC preset.
   */
  public String getPreset() {
    return preset;
  }

  /**
   * Sets the NVENC preset.
   *
   * @param preset the NVENC preset.
   */
  public void setPreset(String preset) {
    this.preset = preset;
  }

  /**
   * Returns the x264 preset.
   *
   * @return the x264 preset.
   */
  public String getX264Preset() {
    return x264Preset;
  }

  /**
   * Sets the x264 preset.
   *
   * @param x264Preset the x264 preset.
   */
  public void setX264Preset(String x264Preset) {
    this.x264Preset = x264Preset;
  }

  /**
   * Returns the temporary output directory.
   *
   * @return the temporary output directory.
   */
  public String getTempDir() {
    return tempDir;
  }

  /**
   * Sets the temporary output directory.
   *
   * @param tempDir the temporary output directory.
   */
  public void setTempDir(String tempDir) {
    this.tempDir = tempDir;
  }

  /**
   * Reports whether the original video is retained.
   *
   * @return true when the original video is retained.
   */
  public boolean isKeepOriginal() {
    return keepOriginal;
  }

  /**
   * Sets whether the original video is retained.
   *
   * @param keepOriginal true to retain the original video.
   */
  public void setKeepOriginal(boolean keepOriginal) {
    this.keepOriginal = keepOriginal;
  }

  /**
   * Returns the maximum number of concurrent transcodes.
   *
   * @return the maximum number of concurrent transcodes.
   */
  public int getMaxConcurrent() {
    return maxConcurrent;
  }

  /**
   * Sets the maximum number of concurrent transcodes.
   *
   * @param maxConcurrent the maximum number of concurrent transcodes.
   */
  public void setMaxConcurrent(int maxConcurrent) {
    this.maxConcurrent = maxConcurrent;
  }
}
