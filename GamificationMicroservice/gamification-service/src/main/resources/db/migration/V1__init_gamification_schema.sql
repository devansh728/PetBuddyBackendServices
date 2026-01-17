-- V1__init_gamification_schema.sql
-- Initial schema for gamification service

-- User gamification state table
CREATE TABLE user_gamification (
    user_id UUID PRIMARY KEY,
    total_points INTEGER NOT NULL DEFAULT 0,
    current_level INTEGER NOT NULL DEFAULT 1,
    level_title VARCHAR(50) NOT NULL DEFAULT 'Pet Newbie',
    login_streak INTEGER NOT NULL DEFAULT 0,
    last_login_date DATE,
    weekly_posts INTEGER NOT NULL DEFAULT 0,
    weekly_likes INTEGER NOT NULL DEFAULT 0,
    weekly_comments INTEGER NOT NULL DEFAULT 0,
    weekly_purchases INTEGER NOT NULL DEFAULT 0,
    weekly_donations INTEGER NOT NULL DEFAULT 0,
    weekly_ai_questions INTEGER NOT NULL DEFAULT 0,
    week_start_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Index for leaderboard queries
CREATE INDEX idx_gamification_points ON user_gamification(total_points DESC);

-- User badges table
CREATE TABLE user_badges (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    badge_id VARCHAR(50) NOT NULL,
    unlocked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, badge_id)
);

CREATE INDEX idx_badges_user ON user_badges(user_id);

-- User challenges completion tracking
CREATE TABLE user_challenges (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    challenge_id VARCHAR(50) NOT NULL,
    completed_date DATE NOT NULL,
    points_earned INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, challenge_id, completed_date)
);

CREATE INDEX idx_challenges_user_date ON user_challenges(user_id, completed_date);

-- Point transactions audit log
CREATE TABLE point_transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    points INTEGER NOT NULL,
    points_before INTEGER NOT NULL,
    points_after INTEGER NOT NULL,
    reference_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transactions_user ON point_transactions(user_id);
CREATE INDEX idx_transactions_created ON point_transactions(created_at DESC);
