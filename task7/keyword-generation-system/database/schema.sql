-- ============================================
-- KEYWORD GENERATION SYSTEM - MYSQL SCHEMA
-- Matches the current keyword_generator database tables.
-- ============================================

CREATE DATABASE IF NOT EXISTS keyword_generator;
USE keyword_generator;

-- ============================================
-- KEYWORDS TABLE
-- ============================================

CREATE TABLE IF NOT EXISTS keywords (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content_id VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    keyword VARCHAR(1000) NOT NULL,
    score DOUBLE NOT NULL,
    search_volume INT NULL,
    type VARCHAR(50) NOT NULL,
    estimated TINYINT(1) NOT NULL DEFAULT 1,
    source VARCHAR(100) NULL,
    validation_status VARCHAR(100) NULL,
    trend_direction DOUBLE NULL,
    validated_at DATETIME(6) NULL
);

-- ============================================
-- KEYWORDS INDEXES
-- MySQL reports these indexed fields as MUL in DESC output.
-- ============================================

CREATE INDEX idx_content_id ON keywords(content_id);
CREATE INDEX idx_created_at ON keywords(created_at);
CREATE INDEX idx_keyword ON keywords(keyword(255));
CREATE INDEX idx_score ON keywords(score);
CREATE INDEX idx_search_volume ON keywords(search_volume);
CREATE INDEX idx_type ON keywords(type);
CREATE INDEX idx_source ON keywords(source);
CREATE INDEX idx_validated_at ON keywords(validated_at);

-- ============================================
-- SEARCH ANALYTICS TABLE
-- ============================================

CREATE TABLE IF NOT EXISTS search_analytics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(500) NOT NULL,
    search_count BIGINT NOT NULL DEFAULT 1,
    last_searched TIMESTAMP NOT NULL,
    total_content_generated INT NOT NULL DEFAULT 0,
    average_score DOUBLE NOT NULL DEFAULT 0,
    popularity_tier VARCHAR(20) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL
);

-- ============================================
-- COLUMN NOTES
-- ============================================

/*
keywords:
- id: Primary key.
- content_id: Hash-like id linking keywords to submitted content.
- created_at: Creation timestamp.
- keyword: Generated keyword phrase.
- score: Final normalized quality/demand score from 0 to 100.
- search_volume: Search volume when reliable volume data exists. NULL/0 may be used for autocomplete-only validation.
- type: SHORT, MEDIUM, or LONG_TAIL.
- estimated: 1 when validation/search volume is estimated or proxy-based.
- source: Validation source such as PYTRENDS_REDIS, GOOGLE_AUTOCOMPLETE, NO_TREND_DATA, or ESTIMATED_FALLBACK.
- validation_status: VALID, ESTIMATED, LOW_VOLUME, LOW_DEMAND, etc.
- trend_direction: Approximate trend movement.
- validated_at: Timestamp when validation was performed.

search_analytics:
- id: Primary key.
- keyword: Search term tracked internally.
- search_count: Number of times this keyword/search has been tracked.
- last_searched: Most recent search timestamp.
- total_content_generated: Number of generated keyword results associated with this search.
- average_score: Rolling average score for generated keywords.
- popularity_tier: HIGH, MEDIUM, or LOW.
- created_at: Row creation timestamp.
- updated_at: Last update timestamp.
*/
