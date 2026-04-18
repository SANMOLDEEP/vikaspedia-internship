-- Create database if it doesn't exist
CREATE DATABASE IF NOT EXISTS fingerprint_db;

-- Use the database
USE fingerprint_db;

-- Create guest_sessions table with bot detection
CREATE TABLE IF NOT EXISTS guest_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fingerprint_id VARCHAR(255) NOT NULL,
    first_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    bot ENUM('YES', 'NO') DEFAULT 'NO',
    indicators TEXT,
    user_id BIGINT,
    visits INT DEFAULT 1,
    INDEX idx_fingerprint (fingerprint_id),
    INDEX idx_last_seen (last_seen),
    INDEX idx_user_id (user_id)
);

-- Create events table
CREATE TABLE IF NOT EXISTS event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fingerprint_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    INDEX idx_fingerprint (fingerprint_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_event_type (event_type)
);

-- Create users table with fingerprint linking
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255),
    fingerprint_id VARCHAR(255),
    INDEX idx_email (email),
    INDEX idx_fingerprint (fingerprint_id)
);

-- Show tables
SHOW TABLES;
