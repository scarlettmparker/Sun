CREATE TABLE IF NOT EXISTS hades_reader_text_counts (
  status varchar(255) PRIMARY KEY,
  total bigint NOT NULL DEFAULT 0
);

INSERT INTO hades_reader_text_counts (status, total)
SELECT status, count(*) FROM hades_reader_texts GROUP BY status
ON CONFLICT (status) DO UPDATE SET total = EXCLUDED.total;

CREATE OR REPLACE FUNCTION hades_reader_text_count_change() RETURNS trigger AS $$
BEGIN
  IF TG_OP = 'INSERT' THEN
    INSERT INTO hades_reader_text_counts (status, total) VALUES (NEW.status, 1)
    ON CONFLICT (status) DO UPDATE SET total = hades_reader_text_counts.total + 1;
  ELSIF TG_OP = 'DELETE' THEN
    UPDATE hades_reader_text_counts SET total = GREATEST(total - 1, 0) WHERE status = OLD.status;
  ELSIF TG_OP = 'UPDATE' AND NEW.status IS DISTINCT FROM OLD.status THEN
    UPDATE hades_reader_text_counts SET total = GREATEST(total - 1, 0) WHERE status = OLD.status;
    INSERT INTO hades_reader_text_counts (status, total) VALUES (NEW.status, 1)
    ON CONFLICT (status) DO UPDATE SET total = hades_reader_text_counts.total + 1;
  END IF;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS hades_reader_text_count_trigger ON hades_reader_texts;
CREATE TRIGGER hades_reader_text_count_trigger
AFTER INSERT OR UPDATE OR DELETE ON hades_reader_texts
FOR EACH ROW EXECUTE FUNCTION hades_reader_text_count_change();
