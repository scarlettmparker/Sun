CREATE INDEX IF NOT EXISTS hades_reader_texts_status_level_idx
  ON hades_reader_texts (status, level, id);

CREATE INDEX IF NOT EXISTS hades_reader_texts_status_owner_level_idx
  ON hades_reader_texts (status, owner_id, level, id);
