-- Seeds the reader table so list queries hit realistic row sizes.
DELETE FROM hades_reader_texts WHERE title LIKE 'Bench text %';

INSERT INTO hades_reader_texts
  (id, createdat, lastupdatedat, owner_id, source_id, content, language, level, status, title)
SELECT
  gen_random_uuid(),
  now() - (g || ' minutes')::interval,
  now() - (g || ' minutes')::interval,
  NULL,
  NULL,
  repeat('The quick brown fox jumps over the lazy dog. ', 40),
  'en',
  (ARRAY['A1','A2','B1','B2','C1','C2'])[1 + (g % 6)],
  CASE WHEN g % 20 = 0 THEN 'ARCHIVED' ELSE 'ACTIVE' END,
  'Bench text ' || g
FROM generate_series(1, 10000) AS g;

ANALYZE hades_reader_texts;
