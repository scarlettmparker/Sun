package com.sun.dionysus.subtitle;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Computes the OpenSubtitles movie hash.
 */
public final class OpenSubtitlesHasher {

  private static final int CHUNK_SIZE = 64 * 1024;

  private OpenSubtitlesHasher() {
  }

  /**
   * Computes the hash for a file.
   *
   * @param file the movie file
   * @return the 16 character hexadecimal hash
   * @throws IOException when the file cannot be read
   */
  public static String hash(Path file) throws IOException {
    if (file == null) {
      throw new IllegalArgumentException("file must not be null");
    }
    long size = Files.size(file);
    byte[] first = readChunk(file, 0, CHUNK_SIZE);
    byte[] last = size <= CHUNK_SIZE
        ? first
        : readChunk(file, size - CHUNK_SIZE, CHUNK_SIZE);
    return hash(size, first, last);
  }

  /**
   * Computes the hash for a file.
   *
   * @param file the movie file
   * @return the 16 character hexadecimal hash
   * @throws IOException when the file cannot be read
   */
  public static String compute(Path file) throws IOException {
    return hash(file);
  }

  /**
   * Computes the hash for a file.
   *
   * @param file the movie file
   * @return the 16 character hexadecimal hash
   * @throws IOException when the file cannot be read
   */
  public static String computeHash(Path file) throws IOException {
    return hash(file);
  }

  /**
   * Computes the hash for a file.
   *
   * @param file the movie file
   * @return the 16 character hexadecimal hash
   * @throws IOException when the file cannot be read
   */
  public static String movieHash(Path file) throws IOException {
    return hash(file);
  }

  /**
   * Computes the hash from its components.
   *
   * @param size the file size in bytes
   * @param firstChunk the first chunk of the file
   * @param lastChunk the last chunk of the file
   * @return the 16 character hexadecimal hash
   */
  public static String hash(long size, byte[] firstChunk, byte[] lastChunk) {
    long sum = size;
    sum += sumChunk(firstChunk);
    sum += sumChunk(lastChunk);
    return String.format("%016x", sum);
  }

  /**
   * Computes the hash from its components.
   *
   * @param size the file size in bytes
   * @param firstChunk the first chunk of the file
   * @param lastChunk the last chunk of the file
   * @return the 16 character hexadecimal hash
   */
  public static String compute(long size, byte[] firstChunk, byte[] lastChunk) {
    return hash(size, firstChunk, lastChunk);
  }

  /**
   * Sums 64-bit little-endian words in a chunk.
   */
  private static long sumChunk(byte[] chunk) {
    if (chunk == null || chunk.length < 8) {
      return 0L;
    }
    long sum = 0L;
    ByteBuffer buffer = ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN);
    int limit = chunk.length - (chunk.length % 8);
    for (int i = 0; i < limit; i += 8) {
      sum += buffer.getLong(i);
    }
    return sum;
  }

  /**
   * Reads a chunk from a file.
   */
  private static byte[] readChunk(Path file, long offset, int length) throws IOException {
    byte[] buffer = new byte[length];
    try (InputStream input = Files.newInputStream(file)) {
      input.skip(offset);
      int read = 0;
      while (read < length) {
        int count = input.read(buffer, read, length - read);
        if (count < 0) {
          break;
        }
        read += count;
      }
      if (read == length) {
        return buffer;
      }
      byte[] result = new byte[read];
      System.arraycopy(buffer, 0, result, 0, read);
      return result;
    }
  }
}
