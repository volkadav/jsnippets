-- liquibase formatted sql

-- changeset jsnippets:v003-1
-- add join table for user follower/following relationships
CREATE TABLE followers (
    follower_id BIGINT NOT NULL,
    followed_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (follower_id, followed_id),
    FOREIGN KEY (follower_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (followed_id) REFERENCES users (id) ON DELETE CASCADE,
    CHECK (follower_id != followed_id)
);

-- Indexes for followers table
-- Primary key (follower_id, followed_id) already creates an index for "who does user X follow?" queries
-- Add reverse index for "who follows user X?" queries
CREATE INDEX idx_followers_followed_id ON followers(followed_id, follower_id);

-- Index for ordering by follow date (most recent follows first)
CREATE INDEX idx_followers_created_at ON followers(created_at DESC);

-- Composite indexes for common query patterns
CREATE INDEX idx_followers_follower_created ON followers(follower_id, created_at DESC);
CREATE INDEX idx_followers_followed_created ON followers(followed_id, created_at DESC);