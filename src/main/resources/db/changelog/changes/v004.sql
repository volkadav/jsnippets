-- Email-to-Snippet feature: Track processed emails
-- liquibase formatted sql

-- changeset jsnippets:v004-1
CREATE TABLE processed_emails
(
    id               BIGSERIAL PRIMARY KEY,
    message_id       TEXT                     NOT NULL UNIQUE,
    sender_email     TEXT,
    processed_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    snippet_id       BIGINT                   REFERENCES snippets (id) ON DELETE SET NULL,
    status           TEXT                     NOT NULL,
    error_message    TEXT,
    original_subject TEXT,
    original_body    TEXT
);

-- changeset jsnippets:v004-2
CREATE INDEX idx_processed_emails_message_id ON processed_emails (message_id);
CREATE INDEX idx_processed_emails_processed_at ON processed_emails (processed_at);
CREATE INDEX idx_processed_emails_sender ON processed_emails (sender_email);