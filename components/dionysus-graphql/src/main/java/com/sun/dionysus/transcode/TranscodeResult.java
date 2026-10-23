package com.sun.dionysus.transcode;

import java.nio.file.Path;

/**
 * Result of a completed transcode.
 *
 * @param output the temporary MP4 file.
 * @param remuxed whether the video stream was copied without re-encoding.
 * @param encoder the encoder used.
 */
public record TranscodeResult(Path output, boolean remuxed, String encoder) {
}
