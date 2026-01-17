-- PetBuddy Backend Services - Database Initialization
-- This script creates all required databases for the microservices

-- Auth Service Database
CREATE DATABASE auth_db;

-- User Profile Service Database
CREATE DATABASE user_profile_db;

-- Interaction Service Database (already exists if using interaction docker-compose)
CREATE DATABASE interaction_db;

-- Social Feed Service Database
CREATE DATABASE social_feed_db;

-- Feed Distribution Service Database
CREATE DATABASE feed_distribution_db;

-- Gamification Service Database
CREATE DATABASE gamification_db;

-- Grant all privileges to postgres user (default)
GRANT ALL PRIVILEGES ON DATABASE auth_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE user_profile_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE interaction_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE social_feed_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE feed_distribution_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE gamification_db TO postgres;

