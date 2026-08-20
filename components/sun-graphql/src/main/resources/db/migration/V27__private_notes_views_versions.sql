-- V27 private notes (own table, remote_object tag), text views, text versions and polymorphic shares.
-- Notes use remote_object ["private_note","hades:text:{textId}"] for locateRemoteObjects.
-- Shares is generic (any ownable) - private_note, comment, text etc. future.

CREATE TABLE hades_private_notes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id UUID NOT NULL REFERENCES gaia_accounts(id) ON DELETE CASCADE,
  text_id UUID NOT NULL REFERENCES hades_reader_texts(id) ON DELETE CASCADE,
  start_offset INT NOT NULL CHECK (start_offset >= 0),
  end_offset INT NOT NULL CHECK (end_offset > start_offset),
  body TEXT NOT NULL CHECK (char_length(body) >= 1),
  visibility TEXT NOT NULL DEFAULT 'PRIVATE' CHECK (visibility IN ('PRIVATE','SHARED')),
  remote_object JSONB NOT NULL DEFAULT '["private_note"]'::jsonb,
  createdat TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  lastupdatedat TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by UUID,
  last_updated_by UUID,
  CONSTRAINT private_note_range CHECK (end_offset > start_offset)
);

CREATE INDEX idx_private_notes_owner ON hades_private_notes(owner_id);
CREATE INDEX idx_private_notes_text ON hades_private_notes(text_id);
CREATE INDEX idx_private_notes_remote_object ON hades_private_notes USING GIN (remote_object);

CREATE TABLE hades_text_views (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  account_id UUID NOT NULL REFERENCES gaia_accounts(id) ON DELETE CASCADE,
  text_id UUID NOT NULL REFERENCES hades_reader_texts(id) ON DELETE CASCADE,
  viewed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  createdat TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  lastupdatedat TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by UUID,
  last_updated_by UUID,
  CONSTRAINT uq_text_views_account_text UNIQUE (account_id, text_id)
);
CREATE INDEX idx_text_views_account ON hades_text_views(account_id, viewed_at DESC);
CREATE INDEX idx_text_views_text ON hades_text_views(text_id);

CREATE TABLE hades_text_versions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  text_id UUID NOT NULL REFERENCES hades_reader_texts(id) ON DELETE CASCADE,
  version INT NOT NULL,
  title TEXT NOT NULL,
  content TEXT NOT NULL,
  level TEXT NOT NULL,
  language TEXT NOT NULL,
  edited_by UUID REFERENCES gaia_accounts(id) ON DELETE SET NULL,
  createdat TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  lastupdatedat TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by UUID,
  last_updated_by UUID,
  CONSTRAINT uq_text_versions_text_version UNIQUE (text_id, version)
);
CREATE INDEX idx_text_versions_text ON hades_text_versions(text_id, version DESC);

-- Polymorphic shares for any ownable (private_note, comment, text, etc.)
CREATE TABLE gaia_object_shares (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  object_type TEXT NOT NULL,
  object_id UUID NOT NULL,
  subject_type TEXT NOT NULL CHECK (subject_type IN ('user','role')),
  subject_id UUID NOT NULL,
  relation TEXT NOT NULL CHECK (relation IN ('VIEWER','EDITOR','OWNER')),
  createdat TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  lastupdatedat TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by UUID,
  last_updated_by UUID,
  CONSTRAINT uq_object_shares UNIQUE (object_type, object_id, subject_type, subject_id)
);
CREATE INDEX idx_object_shares_object ON gaia_object_shares(object_type, object_id);
CREATE INDEX idx_object_shares_subject ON gaia_object_shares(subject_type, subject_id);
