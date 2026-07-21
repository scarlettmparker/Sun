package com.sun.dionysus.graphql.mappers;

import com.sun.dionysus.codegen.types.KeyEntry;
import com.sun.dionysus.codegen.types.TorrentDownload;
import com.sun.dionysus.model.KeyDetailEntity;
import com.sun.dionysus.model.TorrentJobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Mapper for converting S3 list results into GraphQL KeyEntry types.
 * Enriches entries with metadata from KeyDetail records.
 */
@Component
public class KeyEntryMapper {

  private static final Logger logger = LoggerFactory.getLogger(KeyEntryMapper.class);

  /**
   * Maps an S3 common prefix to a directory-style KeyEntry.
   *
   * @param prefix the S3 CommonPrefix
   * @return the mapped KeyEntry with isDirectory=true
   */
  public KeyEntry mapDirectory(CommonPrefix prefix) {
    logger.debug("Mapping directory key entry {}", prefix.prefix());
    KeyEntry entry = new KeyEntry();
    entry.setKey(prefix.prefix());
    entry.setIsDirectory(true);
    entry.setSize(0);
    return entry;
  }

  /**
   * Maps an S3 common prefix to a directory-style KeyEntry with optional KeyDetail metadata.
   *
   * @param prefix the S3 CommonPrefix
   * @param keyDetail optional KeyDetail with metadata
   * @return the mapped KeyEntry with name/description from KeyDetail if available
   */
  public KeyEntry mapDirectory(CommonPrefix prefix, KeyDetailEntity keyDetail) {
    logger.debug("Mapping directory key entry {}", prefix.prefix());
    KeyEntry entry = new KeyEntry();
    entry.setKey(prefix.prefix());
    entry.setIsDirectory(true);
    entry.setSize(0);
    if (keyDetail != null) {
      entry.setName(keyDetail.getName());
      entry.setDescription(keyDetail.getDescription());
    }
    return entry;
  }

  /**
   * Maps an S3 object to a file KeyEntry.
   *
   * @param object the S3Object
   * @return the mapped KeyEntry with isDirectory=false
   */
  public KeyEntry mapFile(S3Object object) {
    logger.debug("Mapping file key entry {}", object.key());
    KeyEntry entry = new KeyEntry();
    entry.setKey(object.key());
    entry.setIsDirectory(false);
    entry.setSize(object.size() != null ? object.size().intValue() : 0);
    if (object.lastModified() != null) {
      entry.setLastModified(object.lastModified().toString());
    }
    return entry;
  }

  /**
   * Maps an S3 object to a file KeyEntry with optional KeyDetail metadata.
   *
   * @param object the S3Object
   * @param keyDetail optional KeyDetail with metadata
   * @return the mapped KeyEntry with name/description from KeyDetail if available
   */
  public KeyEntry mapFile(S3Object object, KeyDetailEntity keyDetail) {
    logger.debug("Mapping file key entry {}", object.key());
    KeyEntry entry = new KeyEntry();
    entry.setKey(object.key());
    entry.setIsDirectory(false);
    entry.setSize(object.size() != null ? object.size().intValue() : 0);
    if (object.lastModified() != null) {
      entry.setLastModified(object.lastModified().toString());
    }
    if (keyDetail != null) {
      entry.setName(keyDetail.getName());
      entry.setDescription(keyDetail.getDescription());
    }
    return entry;
  }

  /**
   * Builds a new KeyEntry from a torrent job, for keys that have no S3 object yet.
   */
  public KeyEntry mapTorrentJob(TorrentJobEntity job) {
    KeyEntry entry = new KeyEntry();
    entry.setKey(job.getTargetKeyPath());
    entry.setIsDirectory(job.getTargetKeyPath().endsWith("/"));
    entry.setSize((int) Math.min(job.getTotalBytes(), Integer.MAX_VALUE));
    if (job.getMagnetDetail() != null) {
      entry.setName(job.getMagnetDetail().getDisplayName());
    }
    entry.setTorrent(toTorrentDownload(job));
    return entry;
  }

  /**
   * Copies torrent download fields onto an existing S3-derived KeyEntry.
   */
  public void mergeTorrentJob(KeyEntry entry, TorrentJobEntity job) {
    entry.setTorrent(toTorrentDownload(job));
  }

  /**
   * Builds the nested download state from a job.
   */
  private TorrentDownload toTorrentDownload(TorrentJobEntity job) {
    TorrentDownload download =
        TorrentDownload.newBuilder()
            .jobId(job.getId().toString())
            .status(job.getStatus().name())
            .progress(job.getProgress())
            .build();
    if (job.getMagnetDetail() != null && job.getMagnetDetail().getId() != null) {
      download.setMagnetDetailId(job.getMagnetDetail().getId().toString());
    }
    if (job.getDownloadRateBps() != null) {
      download.setDownloadRateBps(job.getDownloadRateBps());
    }
    if (job.getEtaSeconds() != null) {
      download.setEtaSeconds(job.getEtaSeconds().intValue());
    }
    if (job.getPeersConnected() != null) {
      download.setPeersConnected(job.getPeersConnected());
    }
    if (job.getErrorMessage() != null) {
      download.setErrorMessage(job.getErrorMessage());
    }
    download.setDownloadedBytes(job.getDownloadedBytes());
    download.setTotalBytes(job.getTotalBytes());
    return download;
  }
}
