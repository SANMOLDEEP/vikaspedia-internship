-- Complete Database Setup for Collaborative Editor
-- Run this script manually in MySQL to create all necessary tables
-- This creates the complete database structure used by the application

-- Create documents table (stores document metadata)
CREATE TABLE IF NOT EXISTS documents (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    content LONGTEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

-- Create users table (stores user information)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) UNIQUE NOT NULL,
    display_name VARCHAR(255),
    email VARCHAR(255),           -- Kept for compatibility but not used
    avatar_url VARCHAR(255),      -- Kept for compatibility but not used
    user_color VARCHAR(255),
    is_guest BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_active DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create content_history table (tracks content changes - last 50 per document)
CREATE TABLE IF NOT EXISTS content_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    username VARCHAR(255),
    content TEXT,
    content_length INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    operation_type VARCHAR(50) NOT NULL,
    character_count INT,
    INDEX idx_document_id (document_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
);

-- Create user_history table (tracks user activity - last 50 globally)
CREATE TABLE IF NOT EXISTS user_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    email VARCHAR(255),           -- Kept for compatibility but not used
    avatar_url VARCHAR(255),      -- Kept for compatibility but not used
    user_color VARCHAR(255),
    is_guest BOOLEAN DEFAULT TRUE,
    document_id VARCHAR(255),
    activity_type VARCHAR(50) DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_active DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_document_id (document_id),
    INDEX idx_last_active (last_active),
    INDEX idx_created_at (created_at)
);


-- Insert sample documents for testing
INSERT INTO documents (id, name, content, created_by) VALUES 
('doc_1663ed3d', 'Document One', 'Welcome to the collaborative editor!', 'system'),
('doc_ae27ae3b', 'Document Two', 'This is a second document for testing.', 'system')
ON DUPLICATE KEY UPDATE name = VALUES(name), content = VALUES(content);

-- Insert sample users for testing (no email/avatar_url since auth is disabled)
INSERT INTO users (user_id, display_name, user_color, is_guest) VALUES 
('user60', 'myname', '#FF6B6B', TRUE),
('user918', 'xyz', '#4ECDC4', TRUE)
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name);

-- Insert sample content history for testing
INSERT INTO content_history (document_id, user_id, username, content, content_length, operation_type, character_count) VALUES
('doc_1663ed3d', 'user60', 'myname', 'Welcome to the collaborative editor!', 31, 'UPDATE', 31),
('doc_ae27ae3b', 'user918', 'xyz', 'This is a second document for testing.', 35, 'UPDATE', 35)
ON DUPLICATE KEY UPDATE content = VALUES(content);

-- Insert sample user history for testing (no email/avatar_url since auth is disabled)
INSERT INTO user_history (user_id, display_name, document_id, activity_type) VALUES
('user60', 'myname', 'doc_1663ed3d', 'ACTIVE'),
('user918', 'xyz', 'doc_ae27ae3d', 'ACTIVE')
ON DUPLICATE KEY UPDATE last_active = CURRENT_TIMESTAMP;

-- Create performance indexes
CREATE INDEX IF NOT EXISTS idx_documents_created_by ON documents(created_by);
CREATE INDEX IF NOT EXISTS idx_documents_updated_at ON documents(updated_at);
CREATE INDEX IF NOT EXISTS idx_users_last_active ON users(last_active);
CREATE INDEX IF NOT EXISTS idx_content_history_document_created ON content_history(document_id, created_at);
CREATE INDEX IF NOT EXISTS idx_user_history_last_active ON user_history(last_active DESC);

-- Verify tables were created
SELECT 'Tables created successfully' as status;

-- Show table structure
SHOW TABLES;

-- Display sample data
SELECT 'documents:' as table_name;
SELECT id, name, LEFT(content, 30) as content_preview, created_by FROM documents LIMIT 3;

SELECT 'users:' as table_name;
SELECT user_id, display_name, user_color, is_guest FROM users LIMIT 3;

SELECT 'content_history:' as table_name;
SELECT document_id, user_id, username, LEFT(content, 20) as content_preview, created_at FROM content_history LIMIT 3;

SELECT 'user_history:' as table_name;
SELECT user_id, display_name, document_id, activity_type, last_active FROM user_history LIMIT 3;
