-- Phase 1: Core Tables for Likes, Comments, and Metrics
-- Version: V1__Initial_Schema.sql
-- Author: PetBuddy Team
-- Date: 2025-11-10

-- ============================================
-- LIKES TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS likes (
    like_id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_likes_post_user UNIQUE (post_id, user_id)
);

-- Indexes for performance
CREATE INDEX idx_likes_post_id ON likes(post_id);
CREATE INDEX idx_likes_user_id ON likes(user_id);
CREATE INDEX idx_likes_created_at ON likes(created_at DESC);

-- Comment on table
COMMENT ON TABLE likes IS 'Stores user likes for posts';
COMMENT ON COLUMN likes.post_id IS 'Reference to the post being liked';
COMMENT ON COLUMN likes.user_id IS 'Reference to the user who liked';

-- ============================================
-- COMMENTS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS comments (
    comment_id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_comment_id BIGINT,
    comment_text VARCHAR(1000) NOT NULL,
    mentioned_users BIGINT[],
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_comment_id)
        REFERENCES comments(comment_id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_comments_post_id ON comments(post_id);
CREATE INDEX idx_comments_user_id ON comments(user_id);
CREATE INDEX idx_comments_parent_id ON comments(parent_comment_id);
CREATE INDEX idx_comments_created_at ON comments(created_at DESC);
CREATE INDEX idx_comments_is_deleted ON comments(is_deleted);

-- Composite index for common queries
CREATE INDEX idx_comments_post_not_deleted ON comments(post_id, is_deleted, created_at DESC);

-- Comment on table
COMMENT ON TABLE comments IS 'Stores user comments and replies on posts';
COMMENT ON COLUMN comments.parent_comment_id IS 'Reference to parent comment for nested replies (null for top-level)';
COMMENT ON COLUMN comments.mentioned_users IS 'Array of user IDs mentioned in the comment (@username)';
COMMENT ON COLUMN comments.is_deleted IS 'Soft delete flag - true if comment is deleted';

-- ============================================
-- POST METRICS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS post_metrics (
    post_id BIGINT PRIMARY KEY,
    like_count BIGINT NOT NULL DEFAULT 0,
    comment_count BIGINT NOT NULL DEFAULT 0,
    share_count BIGINT NOT NULL DEFAULT 0,
    view_count BIGINT NOT NULL DEFAULT 0,
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Index for reconciliation queries
CREATE INDEX idx_post_metrics_updated ON post_metrics(last_updated DESC);

-- Comment on table
COMMENT ON TABLE post_metrics IS 'Aggregated metrics for posts';
COMMENT ON COLUMN post_metrics.version IS 'Optimistic locking version for concurrent updates';
COMMENT ON COLUMN post_metrics.last_updated IS 'Last time metrics were updated';

-- ============================================
-- TRIGGERS FOR AUTO-UPDATE METRICS
-- ============================================

-- Function to update like count
CREATE OR REPLACE FUNCTION update_like_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO post_metrics (post_id, like_count, last_updated)
        VALUES (NEW.post_id, 1, CURRENT_TIMESTAMP)
        ON CONFLICT (post_id)
        DO UPDATE SET
            like_count = post_metrics.like_count + 1,
            last_updated = CURRENT_TIMESTAMP,
            version = post_metrics.version + 1;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE post_metrics
        SET like_count = GREATEST(like_count - 1, 0),
            last_updated = CURRENT_TIMESTAMP,
            version = version + 1
        WHERE post_id = OLD.post_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Trigger for like count
CREATE TRIGGER trigger_update_like_count
AFTER INSERT OR DELETE ON likes
FOR EACH ROW EXECUTE FUNCTION update_like_count();

COMMENT ON FUNCTION update_like_count() IS 'Automatically updates like_count in post_metrics when likes are added/removed';

-- Function to update comment count
CREATE OR REPLACE FUNCTION update_comment_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        -- Only count non-deleted comments
        IF NEW.is_deleted = FALSE THEN
            INSERT INTO post_metrics (post_id, comment_count, last_updated)
            VALUES (NEW.post_id, 1, CURRENT_TIMESTAMP)
            ON CONFLICT (post_id)
            DO UPDATE SET
                comment_count = post_metrics.comment_count + 1,
                last_updated = CURRENT_TIMESTAMP,
                version = post_metrics.version + 1;
        END IF;
    ELSIF TG_OP = 'UPDATE' THEN
        -- If comment was soft deleted
        IF NEW.is_deleted = TRUE AND OLD.is_deleted = FALSE THEN
            UPDATE post_metrics
            SET comment_count = GREATEST(comment_count - 1, 0),
                last_updated = CURRENT_TIMESTAMP,
                version = version + 1
            WHERE post_id = OLD.post_id;
        -- If comment was restored
        ELSIF NEW.is_deleted = FALSE AND OLD.is_deleted = TRUE THEN
            UPDATE post_metrics
            SET comment_count = comment_count + 1,
                last_updated = CURRENT_TIMESTAMP,
                version = version + 1
            WHERE post_id = OLD.post_id;
        END IF;
    ELSIF TG_OP = 'DELETE' THEN
        -- Hard delete (shouldn't happen in normal flow)
        IF OLD.is_deleted = FALSE THEN
            UPDATE post_metrics
            SET comment_count = GREATEST(comment_count - 1, 0),
                last_updated = CURRENT_TIMESTAMP,
                version = version + 1
            WHERE post_id = OLD.post_id;
        END IF;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Trigger for comment count
CREATE TRIGGER trigger_update_comment_count
AFTER INSERT OR UPDATE OR DELETE ON comments
FOR EACH ROW EXECUTE FUNCTION update_comment_count();

COMMENT ON FUNCTION update_comment_count() IS 'Automatically updates comment_count in post_metrics when comments are added/deleted/soft-deleted';

-- ============================================
-- INITIAL DATA (Optional)
-- ============================================

-- Insert default metrics for testing (can be removed in production)
-- This is just for development/testing purposes

