# Real-Time Collaborative Editor

A collaborative text editor built with React, Spring Boot, WebSockets, and MySQL. Multiple users can open same document, edit simultaneously, see active collaborators, and view real-time synchronization with email integration.

## Overview

This project implements a real-time collaborative editing workflow similar to Google Docs:

- Multiple users can edit same document simultaneously
- Edits are sent over WebSocket/STOMP in real time
- Backend maintains canonical document state
- MySQL stores documents and user history
- Active collaborators are shown with unique colors
- Email integration for user identification
- Automatic cleanup for content history (50-entry limit per document)

## Features

- ✅ Real-time collaborative editing
- ✅ Active collaborators panel with unique color identity
- ✅ Email integration for user identification
- ✅ Automatic sync when a user joins a document
- ✅ Automatic reconnect attempts after WebSocket disconnection
- ✅ MySQL persistence for documents and user history
- ✅ User activity tracking and history
- ✅ Mobile/LAN-friendly frontend/backend host configuration
- ✅ Automatic cleanup of content history (50-entry limit per document)
- ✅ Responsive design for mobile and desktop

## Tech Stack

### Frontend

- React 18
- Vite
- SockJS
- STOMP
- Custom CSS
- Simple textarea editor

### Backend

- Spring Boot 3
- Spring WebSocket / STOMP message broker
- Spring Data JPA
- MySQL 8
- Maven

### Database

- MySQL 8

## Architecture

### High-Level Flow

1. User joins app with username and email.
2. User opens a document.
3. Frontend loads latest saved document state through REST.
4. Frontend connects to WebSocket endpoint and subscribes to document topics.
5. Backend registers user as an active collaborator.
6. Users type in editor and changes are sent to backend.
7. Backend applies CRDT operations and broadcasts updates to all clients.
8. Other clients receive updates and apply them immediately.

## Synchronization Model

The implementation uses CRDT (Conflict-Free Replicated Data Types) for conflict resolution:

- Commutative operations ensure order doesn't matter
- Server maintains authoritative document state
- Automatic conflict resolution without data loss
- Real-time synchronization across all clients

## WebSocket Message Flow

### Client to Server

Join:

```json
{
  "documentId": "doc_123",
  "userId": "user-abc",
  "operation": "JOIN",
  "text": {
    "displayName": "John Doe",
    "email": "john@example.com",
    "isGuest": true
  }
}
```

Edit:

```json
{
  "documentId": "doc_123",
  "userId": "user-abc",
  "operation": "EDIT",
  "text": "Updated content"
}
```

### Server to Client

- `/topic/document/{documentId}` - Real-time edits and updates
- `/topic/users/{documentId}` - Active user presence
- `/topic/ping/{documentId}` - Heartbeat for connection maintenance

## Database Schema

### MySQL Tables

**documents**:
- id (VARCHAR, Primary Key)
- name (VARCHAR)
- content (LONGTEXT)
- created_at (DATETIME)
- updated_at (DATETIME)
- created_by (VARCHAR)

**users**:
- id (BIGINT, Primary Key, Auto-increment)
- user_id (VARCHAR, Unique)
- display_name (VARCHAR)
- email (VARCHAR)
- avatar_url (VARCHAR)
- user_color (VARCHAR)
- is_guest (BOOLEAN)
- created_at (DATETIME)
- last_active (DATETIME)

**user_history**:
- id (BIGINT, Primary Key, Auto-increment)
- user_id (VARCHAR)
- display_name (VARCHAR)
- email (VARCHAR)
- avatar_url (VARCHAR)
- document_id (VARCHAR)
- activity_type (VARCHAR)
- user_color (VARCHAR)
- is_guest (BOOLEAN)
- created_at (DATETIME)
- last_active (DATETIME)

**content_history**:
- id (BIGINT, Primary Key, Auto-increment)
- document_id (VARCHAR)
- user_id (VARCHAR)
- username (VARCHAR)
- content (TEXT)
- content_length (INT)
- operation_type (VARCHAR)
- character_count (INT)
- created_at (DATETIME)

## Local Setup

### Prerequisites

- Java 17+
- Node.js 18+
- MySQL 8.0+ running on `localhost:3306`

### Database Setup

1. Start MySQL server
2. Create database:
   ```sql
   CREATE DATABASE collaborative_editor;
   ```
3. Run setup script:
   ```bash
   cd e:\collaborative-editor\backend\editor
   mysql -u root -p collaborative_editor < create_tables.sql
   ```

### Environment Variables

Create environment variables for database connection:

```bash
# Windows (Command Prompt)
set DB_URL=jdbc:mysql://localhost:3306/collab_editor
set DB_USERNAME=root
set DB_PASSWORD=your_actual_password

# Windows (PowerShell)
$env:DB_URL="jdbc:mysql://localhost:3306/collab_editor"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_actual_password"

# Linux/Mac
export DB_URL=jdbc:mysql://localhost:3306/collab_editor
export DB_USERNAME=root
export DB_PASSWORD=your_actual_password
```

### Start Backend

```powershell
cd e:\collaborative-editor\backend\editor
.\mvnw.cmd spring-boot:run
```

Backend runs on:
```
http://localhost:8081
```

### Start Frontend

```powershell
cd e:\collaborative-editor\frontend
npm install
npm run dev
```

Frontend runs on:
```
http://localhost:5173
```

## Mobile / Same-Network Usage

To open app from a phone on same Wi-Fi or hotspot:

```powershell
cd e:\collaborative-editor\frontend
npm run dev -- --host 0.0.0.0
```

Then open:
```
http://YOUR_LAPTOP_IP:5173
```

The frontend automatically detects mobile devices and connects to appropriate backend URL.

## How to Use

1. Open app in browser.
2. Enter your name and optional email.
3. Click "Start Editing".
4. Create a new document or open an existing one.
5. Open same document in another browser tab/device.
6. Start typing in both windows to test live collaboration.

## API Endpoints

### Document Management
- `GET /api/documents/list` - List all documents
- `POST /api/documents/create` - Create new document
- `GET /api/documents/{id}` - Get document content
- `PUT /api/documents/{id}` - Update document

### User History
- `GET /api/user-history/{documentId}` - Get user activity history
- `DELETE /api/test/cleanup-user-history` - Manual cleanup of user history
- `GET /api/test/count` - Get table counts for debugging

### Content History
- `GET /api/content-history/{documentId}` - Get document version history
- `DELETE /api/content-history/cleanup/{documentId}` - Manual cleanup of content history (automatic cleanup maintains 50-entry limit per document)


## Troubleshooting

### Backend does not start
- Verify MySQL is running on `localhost:3306` 
- Check port `8081` is free
- Run `netstat -ano | findstr :8081` to check for conflicts

### Frontend cannot connect
- Verify backend is running on `http://localhost:8081` 
- Check browser console for CORS or WebSocket errors
- Ensure database tables exist

### Collaboration does not sync
- Confirm both users opened same document
- Check editor status shows "Connected"
- Refresh both tabs after restarting backend
- Verify WebSocket connection in browser dev tools

### Port conflicts
```powershell
# Kill process using port 8081
netstat -ano | findstr :8081
taskkill /F /PID <PID>
```

## Key Features Demonstrated

- **Real-time Collaboration**: Instant updates across all connected clients
- **Conflict Resolution**: CRDT ensures no data loss during concurrent edits
- **User Presence**: Active users shown with unique colors and emails
- **Persistence**: All changes saved to MySQL with automatic cleanup for content history
- **Responsive Design**: Works seamlessly on desktop and mobile devices
- **Email Integration**: Users can provide email for better identification
- **Connection Management**: Automatic reconnection and heartbeat system

This collaborative editor provides a complete real-time editing experience with enterprise-grade features including email integration, automatic content history cleanup, and robust conflict resolution.
