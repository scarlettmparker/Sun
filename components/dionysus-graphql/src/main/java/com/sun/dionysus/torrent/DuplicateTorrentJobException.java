package com.sun.dionysus.torrent;

/**
 * Thrown when a torrent job already targets the same bucket key path.
 */
public class DuplicateTorrentJobException extends RuntimeException {

  public DuplicateTorrentJobException(String message) {
    super(message);
  }
}
