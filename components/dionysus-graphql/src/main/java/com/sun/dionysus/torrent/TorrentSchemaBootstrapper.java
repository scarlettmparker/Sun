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

  private static final String DROP_INFOHASH_UNIQUE_SQL = """
      DO $$ BEGIN
        EXECUTE (
          SELECT 'ALTER TABLE dionysus_magnet_detail DROP CONSTRAINT IF EXISTS ' || c.conname
          FROM pg_constraint c
          JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY(c.conkey)
          WHERE c.conrelid = 'dionysus_magnet_detail'::regclass
            AND c.contype = 'u'
            AND a.attname = 'infohash'
        );
      EXCEPTION WHEN OTHERS THEN END;
      $$;
      """;

  /**
   * Drops the unique constraint on key_detail_id in dionysus_torrent_job.
   */
  private static final String DROP_KEYDETAIL_UNIQUE_SQL = """
      DO $$ BEGIN
        EXECUTE (
          SELECT 'ALTER TABLE dionysus_torrent_job DROP CONSTRAINT IF EXISTS ' || c.conname
          FROM pg_constraint c
          JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY(c.conkey)
          WHERE c.conrelid = 'dionysus_torrent_job'::regclass
            AND c.contype = 'u'
            AND a.attname = 'key_detail_id'
        );
      EXCEPTION WHEN OTHERS THEN END;
      $$;
      """;

  private final JdbcTemplate jdbcTemplate;

  public TorrentSchemaBootstrapper(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void run(ApplicationArguments args) {
    jdbcTemplate.execute(DROP_INFOHASH_UNIQUE_SQL);
    jdbcTemplate.execute(DROP_KEYDETAIL_UNIQUE_SQL);
    jdbcTemplate.execute(CREATE_INDEX_SQL);
    logger.info("Ensured torrent one-active-job-per-key index exists");
  }
}
