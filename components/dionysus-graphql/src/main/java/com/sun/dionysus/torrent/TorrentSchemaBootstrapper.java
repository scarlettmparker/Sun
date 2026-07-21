package com.sun.dionysus.torrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Creates the partial unique index that enforces at most one unfinished torrent
 * job per bucket key path. JPA cannot express a partial index, and Hibernate's
 * ddl-auto cannot create one, so it is applied directly on startup.
 */
@Component
public class TorrentSchemaBootstrapper implements ApplicationRunner {

  private static final Logger logger = LoggerFactory.getLogger(TorrentSchemaBootstrapper.class);

  private static final String CREATE_INDEX_SQL = """
      CREATE UNIQUE INDEX IF NOT EXISTS uq_active_job_per_key
      ON dionysus_torrent_job (bucket, targetKeyPath)
      WHERE status IN ('QUEUED','METADATA','DOWNLOADING','PAUSED','UPLOADING')
      """;

  private final JdbcTemplate jdbcTemplate;

  public TorrentSchemaBootstrapper(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void run(ApplicationArguments args) {
    jdbcTemplate.execute(CREATE_INDEX_SQL);
    logger.info("Ensured torrent one-active-job-per-key index exists");
  }
}
