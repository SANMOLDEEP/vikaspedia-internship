# Database Schema

This document contains the complete database schema for the Guest Fingerprinting application.

## Database Setup

```sql
-- Create database if it doesn't exist
CREATE DATABASE IF NOT EXISTS fingerprint_db;

-- Use the database
USE fingerprint_db;
```

## Tables

### guest_session Table

Stores session information for guest users with bot detection capabilities.

```sql
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
```

**Fields:**
- `id`: Primary key for the session
- `fingerprint_id`: Unique identifier for the guest's browser fingerprint
- `first_seen`: Timestamp when the session was first created
- `last_seen`: Timestamp that updates on each activity
- `ip_address`: Client IP address
- `user_agent`: Browser user agent string
- `bot`: Enum indicating if the session is detected as a bot ('YES' or 'NO')
- `indicators`: Text field storing bot detection indicators
- `user_id`: Foreign key linking to registered users (nullable)
- `visits`: Counter for total visits by this fingerprint

### event Table

Stores various events tracked for guest users.

```sql
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
```

**Fields:**
- `id`: Primary key for the event
- `fingerprint_id`: Links the event to a specific guest fingerprint
- `event_type`: Type of event (e.g., 'PAGE_VIEW', 'BUTTON_CLICK', 'GUEST_LOGIN', 'USER_LOGIN')
- `timestamp`: When the event occurred
- `ip_address`: Client IP address at the time of event
- `user_agent`: Browser user agent string

### user Table

Stores registered user information with fingerprint linking.

```sql
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255),
    fingerprint_id VARCHAR(255),
    INDEX idx_email (email),
    INDEX idx_fingerprint (fingerprint_id)
);
```

**Fields:**
- `id`: Primary key for the user
- `email`: Unique email address for the user
- `name`: User's display name
- `fingerprint_id`: Links the user to their browser fingerprint

## Verification

```sql
-- Show tables
SHOW TABLES;
```

## Usage Notes

1. **Bot Detection**: The `guest_session` table includes bot detection fields (`bot` and `indicators`) to identify automated traffic.

2. **Fingerprint Linking**: Both `guest_session` and `user` tables can be linked via `fingerprint_id` to track user behavior across sessions.

3. **Event Tracking**: The `event` table captures various user interactions for analytics purposes.

4. **Session Updates**: The `last_seen` timestamp automatically updates on session activity, and `visits` counter increments on each return.

## Security Considerations

- Ensure proper indexing for performance with large datasets
- Consider implementing data retention policies for old sessions
- Regular backups of the database are recommended
- Sensitive data like IP addresses should be handled according to privacy regulations
