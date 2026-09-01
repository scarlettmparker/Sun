-- V42 creates immutable jocasta questions and answers (no updatedAt).
CREATE TABLE jocasta_questions (
  id UUID PRIMARY KEY,
  stem TEXT NOT NULL,
  answer TEXT NOT NULL,
  explanation TEXT,
  remote_object JSONB NOT NULL DEFAULT '[]'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by UUID
);

CREATE INDEX idx_jocasta_questions_remote_object ON jocasta_questions USING GIN (remote_object);
CREATE INDEX idx_jocasta_questions_created_by ON jocasta_questions(created_by);

CREATE TABLE jocasta_answers (
  id UUID PRIMARY KEY,
  question_id UUID NOT NULL REFERENCES jocasta_questions(id) ON DELETE CASCADE,
  my_answer TEXT NOT NULL,
  correct BOOLEAN NOT NULL,
  correct_answer TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by UUID
);

CREATE INDEX idx_jocasta_answers_question_id ON jocasta_answers(question_id, created_at);
CREATE INDEX idx_jocasta_answers_created_by ON jocasta_answers(created_by);
CREATE INDEX idx_jocasta_answers_question_correct ON jocasta_answers(question_id, correct);
