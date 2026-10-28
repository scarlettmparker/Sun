-- V62 stores archived Free Chess games and their moves in Medusa.

CREATE TABLE IF NOT EXISTS medusa_games (
  id uuid PRIMARY KEY,
  code varchar(7) NOT NULL UNIQUE,
  board_layout_id uuid,
  board_layout_name varchar(255) NOT NULL,
  status varchar(255) NOT NULL,
  initial_state jsonb NOT NULL,
  final_state jsonb NOT NULL,
  creator_account_id uuid NOT NULL,
  white_account_id uuid,
  black_account_id uuid,
  result varchar(255),
  winner_account_id uuid,
  move_count integer NOT NULL,
  game_created_at timestamp(6) NOT NULL,
  started_at timestamp(6),
  finished_at timestamp(6) NOT NULL,
  archived_at timestamp(6) NOT NULL,
  createdat timestamp(6) NOT NULL DEFAULT current_timestamp,
  lastupdatedat timestamp(6) NOT NULL DEFAULT current_timestamp,
  created_by uuid,
  last_updated_by uuid
);

CREATE INDEX IF NOT EXISTS medusa_games_creator_account_idx
  ON medusa_games (creator_account_id);

CREATE INDEX IF NOT EXISTS medusa_games_white_account_idx
  ON medusa_games (white_account_id);

CREATE INDEX IF NOT EXISTS medusa_games_black_account_idx
  ON medusa_games (black_account_id);

CREATE INDEX IF NOT EXISTS medusa_games_finished_at_idx
  ON medusa_games (finished_at DESC);

CREATE TABLE IF NOT EXISTS medusa_game_moves (
  game_id uuid NOT NULL REFERENCES medusa_games (id) ON DELETE CASCADE,
  ply integer NOT NULL,
  move bigint NOT NULL,
  played_at timestamp(6) NOT NULL,
  PRIMARY KEY (game_id, ply)
);
