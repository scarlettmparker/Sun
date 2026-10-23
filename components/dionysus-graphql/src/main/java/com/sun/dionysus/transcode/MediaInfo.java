package com.sun.dionysus.transcode;

/**
 * Probed metadata for an input video.
 *
 * @param videoCodec the video codec name.
 * @param height the video height in pixels.
 * @param pixelFormat the video pixel format.
 * @param audioCodec the first audio codec name, or null when there is no audio.
 * @param durationSeconds the container duration in seconds.
 */
record MediaInfo(
    String videoCodec,
    int height,
    String pixelFormat,
    String audioCodec,
    double durationSeconds) {
}
