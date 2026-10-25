-- Records created while no caller identity was available have no owner, so the
-- ownership-filtered list queries hide them. Attach the primary account.

UPDATE medusa_piece_definitions p
SET created_by = a.id
FROM gaia_accounts a
WHERE a.username = 'hi@scarlettparker.co.uk'
  AND p.created_by IS NULL;

UPDATE medusa_piece_sets s
SET created_by = a.id
FROM gaia_accounts a
WHERE a.username = 'hi@scarlettparker.co.uk'
  AND s.created_by IS NULL;

UPDATE medusa_board_layouts l
SET created_by = a.id
FROM gaia_accounts a
WHERE a.username = 'hi@scarlettparker.co.uk'
  AND l.created_by IS NULL;
