-- ============================================================================
-- Like & Comment Service Database Migrations
-- Database: PostgreSQL 15+
-- ============================================================================

-- ============================================================================
-- V1: Create Likes Table
-- ============================================================================

CREATE TABLE IF NOT EXISTS likes (
    like_id         BIGSERIAL PRIMARY KEY,
    post_id         BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT unique_user_post_like UNIQUE (post_id, user_id)
);

-- Indexes for performance
CREATE INDEX idx_likes_post_id ON likes(post_id, created_at DESC);
CREATE INDEX idx_likes_user_id ON likes(user_id, created_at DESC);
CREATE INDEX idx_likes_created_at ON likes(created_at DESC);

-- Partitioning for scalability (optional, for high volume)
-- CREATE TABLE likes_2024 PARTITION OF likes
--     FOR VALUES FROM (MINVALUE) TO ('2025-01-01');

COMMENT ON TABLE likes IS 'Stores user likes on posts';
COMMENT ON COLUMN likes.like_id IS 'Unique identifier for the like';
COMMENT ON COLUMN likes.post_id IS 'Reference to the post being liked';
COMMENT ON COLUMN likes.user_id IS 'Reference to the user who liked';

-- ============================================================================
-- V2: Create Comments Table
-- ============================================================================

CREATE TABLE IF NOT EXISTS comments (
    comment_id          BIGSERIAL PRIMARY KEY,
    post_id             BIGINT NOT NULL,
    user_id             BIGINT NOT NULL,
    parent_comment_id   BIGINT NULL,
    comment_text        TEXT NOT NULL CHECK (length(comment_text) > 0 AND length(comment_text) <= 1000),
    mentioned_users     BIGINT[] DEFAULT '{}',
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,

    -- Self-referencing foreign key for nested replies
    CONSTRAINT fk_parent_comment FOREIGN KEY (parent_comment_id)
        REFERENCES comments(comment_id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_comments_post_id ON comments(post_id, created_at DESC) WHERE is_deleted = FALSE;
CREATE INDEX idx_comments_user_id ON comments(user_id, created_at DESC) WHERE is_deleted = FALSE;
CREATE INDEX idx_comments_parent_id ON comments(parent_comment_id, created_at ASC) WHERE parent_comment_id IS NOT NULL;
CREATE INDEX idx_comments_mentioned ON comments USING GIN (mentioned_users) WHERE array_length(mentioned_users, 1) > 0;

-- Full-text search index for comment content
CREATE INDEX idx_comments_fulltext ON comments USING GIN (to_tsvector('english', comment_text))
    WHERE is_deleted = FALSE;

COMMENT ON TABLE comments IS 'Stores user comments on posts with nested reply support';
COMMENT ON COLUMN comments.parent_comment_id IS 'NULL for top-level comments, references parent for replies';
COMMENT ON COLUMN comments.mentioned_users IS 'Array of user IDs mentioned in the comment (@mentions)';
COMMENT ON COLUMN comments.is_deleted IS 'Soft delete flag - preserves comment structure';

-- ============================================================================
-- V3: Create Post Metrics Table (Aggregated Counts)
-- ============================================================================

CREATE TABLE IF NOT EXISTS post_metrics (
    post_id         BIGINT PRIMARY KEY,
    like_count      BIGINT NOT NULL DEFAULT 0 CHECK (like_count >= 0),
    comment_count   BIGINT NOT NULL DEFAULT 0 CHECK (comment_count >= 0),
    share_count     BIGINT NOT NULL DEFAULT 0 CHECK (share_count >= 0),
    view_count      BIGINT NOT NULL DEFAULT 0 CHECK (view_count >= 0),
    last_updated    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         BIGINT NOT NULL DEFAULT 0  -- Optimistic locking
);

-- Index for finding recently updated posts
CREATE INDEX idx_metrics_updated ON post_metrics(last_updated DESC);

COMMENT ON TABLE post_metrics IS 'Aggregated metrics for posts (denormalized for performance)';
COMMENT ON COLUMN post_metrics.version IS 'Optimistic locking version for concurrent updates';

-- ============================================================================
-- V4: Create Triggers for Automatic Updates
-- ============================================================================

-- Trigger to update comment updated_at timestamp
CREATE OR REPLACE FUNCTION update_comment_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_comment_timestamp
    BEFORE UPDATE ON comments
    FOR EACH ROW
    EXECUTE FUNCTION update_comment_timestamp();

-- Trigger to maintain metrics table on like insert
CREATE OR REPLACE FUNCTION increment_like_count()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO post_metrics (post_id, like_count, last_updated)
    VALUES (NEW.post_id, 1, CURRENT_TIMESTAMP)
    ON CONFLICT (post_id)
    DO UPDATE SET
        like_count = post_metrics.like_count + 1,
        last_updated = CURRENT_TIMESTAMP,
        version = post_metrics.version + 1;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_increment_like_count
    AFTER INSERT ON likes
    FOR EACH ROW
    EXECUTE FUNCTION increment_like_count();

-- Trigger to maintain metrics table on like delete
CREATE OR REPLACE FUNCTION decrement_like_count()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE post_metrics
    SET
        like_count = GREATEST(0, like_count - 1),
        last_updated = CURRENT_TIMESTAMP,
        version = version + 1
    WHERE post_id = OLD.post_id;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_decrement_like_count
    AFTER DELETE ON likes
    FOR EACH ROW
    EXECUTE FUNCTION decrement_like_count();

-- Trigger to maintain metrics table on comment insert
CREATE OR REPLACE FUNCTION increment_comment_count()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_deleted = FALSE THEN
        INSERT INTO post_metrics (post_id, comment_count, last_updated)
        VALUES (NEW.post_id, 1, CURRENT_TIMESTAMP)
        ON CONFLICT (post_id)
        DO UPDATE SET
            comment_count = post_metrics.comment_count + 1,
            last_updated = CURRENT_TIMESTAMP,
            version = post_metrics.version + 1;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_increment_comment_count
    AFTER INSERT ON comments
    FOR EACH ROW
    EXECUTE FUNCTION increment_comment_count();

-- Trigger to maintain metrics table on comment soft delete
CREATE OR REPLACE FUNCTION adjust_comment_count()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.is_deleted = FALSE AND NEW.is_deleted = TRUE THEN
        -- Comment deleted
        UPDATE post_metrics
        SET
            comment_count = GREATEST(0, comment_count - 1),
            last_updated = CURRENT_TIMESTAMP,
            version = version + 1
        WHERE post_id = NEW.post_id;
    ELSIF OLD.is_deleted = TRUE AND NEW.is_deleted = FALSE THEN
        -- Comment restored
        UPDATE post_metrics
        SET
            comment_count = comment_count + 1,
            last_updated = CURRENT_TIMESTAMP,
            version = version + 1
        WHERE post_id = NEW.post_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_adjust_comment_count
    AFTER UPDATE OF is_deleted ON comments
    FOR EACH ROW
    EXECUTE FUNCTION adjust_comment_count();

-- ============================================================================
-- V5: Create Utility Functions
-- ============================================================================

-- Function to get post metrics (used for reconciliation)
CREATE OR REPLACE FUNCTION get_actual_post_metrics(p_post_id BIGINT)
RETURNS TABLE (
    post_id BIGINT,
    actual_like_count BIGINT,
    actual_comment_count BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        p_post_id,
        (SELECT COUNT(*) FROM likes WHERE post_id = p_post_id),
        (SELECT COUNT(*) FROM comments WHERE post_id = p_post_id AND is_deleted = FALSE);
END;
$$ LANGUAGE plpgsql;

-- Function to reconcile metrics (admin/maintenance)
CREATE OR REPLACE FUNCTION reconcile_post_metrics(p_post_id BIGINT)
RETURNS VOID AS $$
DECLARE
    v_actual_likes BIGINT;
    v_actual_comments BIGINT;
BEGIN
    SELECT actual_like_count, actual_comment_count
    INTO v_actual_likes, v_actual_comments
    FROM get_actual_post_metrics(p_post_id);

    INSERT INTO post_metrics (post_id, like_count, comment_count, last_updated)
    VALUES (p_post_id, v_actual_likes, v_actual_comments, CURRENT_TIMESTAMP)
    ON CONFLICT (post_id)
    DO UPDATE SET
        like_count = v_actual_likes,
        comment_count = v_actual_comments,
        last_updated = CURRENT_TIMESTAMP,
        version = post_metrics.version + 1;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- V6: Create Views for Common Queries
-- ============================================================================

-- View: Recent likes with user info (would join with user service in real app)
CREATE OR REPLACE VIEW recent_post_likes AS
SELECT
    l.like_id,
    l.post_id,
    l.user_id,
    l.created_at,
    ROW_NUMBER() OVER (PARTITION BY l.post_id ORDER BY l.created_at DESC) as like_rank
FROM likes l
ORDER BY l.post_id, l.created_at DESC;

-- View: Comment threads with reply counts
CREATE OR REPLACE VIEW comment_threads AS
SELECT
    c.comment_id,
    c.post_id,
    c.user_id,
    c.parent_comment_id,
    c.comment_text,
    c.created_at,
    c.updated_at,
    (SELECT COUNT(*)
     FROM comments replies
     WHERE replies.parent_comment_id = c.comment_id
     AND replies.is_deleted = FALSE) as reply_count,
    c.is_deleted
FROM comments c
WHERE c.is_deleted = FALSE;

-- View: Post engagement summary
CREATE OR REPLACE VIEW post_engagement AS
SELECT
    pm.post_id,
    pm.like_count,
    pm.comment_count,
    pm.share_count,
    pm.view_count,
    (pm.like_count + pm.comment_count * 2 + pm.share_count * 3) as engagement_score,
    pm.last_updated
FROM post_metrics pm;

-- ============================================================================
-- V7: Sample Data for Testing (OPTIONAL - Remove in production)
-- ============================================================================

-- Insert sample metrics for testing
-- INSERT INTO post_metrics (post_id, like_count, comment_count, view_count)
-- VALUES
--     (1, 0, 0, 0),
--     (2, 0, 0, 0),
--     (3, 0, 0, 0);

-- ============================================================================
-- V8: Performance Tuning
-- ============================================================================

-- Analyze tables for query planner
ANALYZE likes;
ANALYZE comments;
ANALYZE post_metrics;

-- Vacuum tables
VACUUM ANALYZE likes;
VACUUM ANALYZE comments;
VACUUM ANALYZE post_metrics;

-- Set statistics target for better query planning
ALTER TABLE likes ALTER COLUMN post_id SET STATISTICS 1000;
ALTER TABLE comments ALTER COLUMN post_id SET STATISTICS 1000;

-- ============================================================================
-- V9: Monitoring & Maintenance Queries
-- ============================================================================

-- Query to find posts with metrics mismatch
CREATE OR REPLACE VIEW metrics_audit AS
SELECT
    pm.post_id,
    pm.like_count as cached_likes,
    COALESCE(l.actual_likes, 0) as actual_likes,
    pm.comment_count as cached_comments,
    COALESCE(c.actual_comments, 0) as actual_comments,
    ABS(pm.like_count - COALESCE(l.actual_likes, 0)) as like_diff,
    ABS(pm.comment_count - COALESCE(c.actual_comments, 0)) as comment_diff
FROM post_metrics pm
LEFT JOIN (
    SELECT post_id, COUNT(*) as actual_likes
    FROM likes
    GROUP BY post_id
) l ON pm.post_id = l.post_id
LEFT JOIN (
    SELECT post_id, COUNT(*) as actual_comments
    FROM comments
    WHERE is_deleted = FALSE
    GROUP BY post_id
) c ON pm.post_id = c.post_id
WHERE
    pm.like_count != COALESCE(l.actual_likes, 0)
    OR pm.comment_count != COALESCE(c.actual_comments, 0);

-- ============================================================================
-- Rollback Scripts (Keep for reference)
-- ============================================================================

/*
-- Rollback V9
DROP VIEW IF EXISTS metrics_audit;

-- Rollback V8
-- (No rollback needed for ANALYZE/VACUUM)

-- Rollback V7
-- DELETE FROM post_metrics WHERE post_id IN (1, 2, 3);

-- Rollback V6
DROP VIEW IF EXISTS post_engagement;
DROP VIEW IF EXISTS comment_threads;
DROP VIEW IF EXISTS recent_post_likes;

-- Rollback V5
DROP FUNCTION IF EXISTS reconcile_post_metrics(BIGINT);
DROP FUNCTION IF EXISTS get_actual_post_metrics(BIGINT);

-- Rollback V4
DROP TRIGGER IF EXISTS trigger_adjust_comment_count ON comments;
DROP TRIGGER IF EXISTS trigger_increment_comment_count ON comments;
DROP TRIGGER IF EXISTS trigger_decrement_like_count ON likes;
DROP TRIGGER IF EXISTS trigger_increment_like_count ON likes;
DROP TRIGGER IF EXISTS trigger_update_comment_timestamp ON comments;
DROP FUNCTION IF EXISTS adjust_comment_count();
DROP FUNCTION IF EXISTS increment_comment_count();
DROP FUNCTION IF EXISTS decrement_like_count();
DROP FUNCTION IF EXISTS increment_like_count();
DROP FUNCTION IF EXISTS update_comment_timestamp();

-- Rollback V3
DROP TABLE IF EXISTS post_metrics;

-- Rollback V2
DROP TABLE IF EXISTS comments;

-- Rollback V1
DROP TABLE IF EXISTS likes;
*/

-- ============================================================================
-- End of Migrations
-- ============================================================================

