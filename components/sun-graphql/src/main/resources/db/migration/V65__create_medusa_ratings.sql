-- V65 stores Glicko-2 ratings and rating history for archived Free Chess games.

CREATE TABLE IF NOT EXISTS medusa_player_ratings (
  account_id uuid PRIMARY KEY,
  rating double precision NOT NULL,
  deviation double precision NOT NULL,
  volatility double precision NOT NULL,
  rated_games integer NOT NULL DEFAULT 0,
  peak_rating double precision NOT NULL,
  last_rated_at timestamp(6),
  created_at timestamp(6) NOT NULL DEFAULT current_timestamp,
  updated_at timestamp(6) NOT NULL DEFAULT current_timestamp
);

CREATE INDEX IF NOT EXISTS medusa_player_ratings_rating_idx
  ON medusa_player_ratings (rating DESC, rated_games DESC);

CREATE TABLE IF NOT EXISTS medusa_rating_history (
  id uuid PRIMARY KEY,
  account_id uuid NOT NULL,
  opponent_account_id uuid NOT NULL,
  game_id uuid NOT NULL REFERENCES medusa_games (id) ON DELETE CASCADE,
  result varchar(32) NOT NULL,
  rating_before double precision NOT NULL,
  rating_after double precision NOT NULL,
  deviation_before double precision NOT NULL,
  deviation_after double precision NOT NULL,
  delta double precision NOT NULL,
  created_at timestamp(6) NOT NULL,
  UNIQUE (game_id, account_id)
);

CREATE INDEX IF NOT EXISTS medusa_rating_history_account_idx
  ON medusa_rating_history (account_id, created_at DESC);

CREATE INDEX IF NOT EXISTS medusa_rating_history_pair_idx
  ON medusa_rating_history (account_id, opponent_account_id, created_at DESC);

ALTER TABLE medusa_games ADD COLUMN IF NOT EXISTS rated boolean NOT NULL DEFAULT false;
ALTER TABLE medusa_games ADD COLUMN IF NOT EXISTS unrated_reason varchar(64);
ALTER TABLE medusa_games ADD COLUMN IF NOT EXISTS white_rating_before double precision;
ALTER TABLE medusa_games ADD COLUMN IF NOT EXISTS white_rating_after double precision;
ALTER TABLE medusa_games ADD COLUMN IF NOT EXISTS white_rating_delta double precision;
ALTER TABLE medusa_games ADD COLUMN IF NOT EXISTS black_rating_before double precision;
ALTER TABLE medusa_games ADD COLUMN IF NOT EXISTS black_rating_after double precision;
ALTER TABLE medusa_games ADD COLUMN IF NOT EXISTS black_rating_delta double precision;
