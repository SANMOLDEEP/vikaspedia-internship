# Guest Fingerprinting System

## Overview

A comprehensive visitor tracking and analytics system with advanced fingerprinting capabilities. The system combines modern browser fingerprinting technology with a professional dashboard to track user behavior, detect bots, and provide detailed visitor insights.

## Features

### Advanced Fingerprinting System
- **Browser Fingerprinting**: Unique visitor identification using multiple browser attributes
- **Bot Detection**: Advanced algorithms to identify automated traffic with real-time scoring
- **Session Tracking**: Comprehensive session management with visit counting
- **Event Tracking**: Monitor user interactions, page views, and login events
- **Account Stitching**: Link multiple sessions to user accounts
- **Collision Detection**: Identify duplicate fingerprint occurrences

### Professional Dashboard
- **Grid-Based Layout**: Modern 4x3 grid system for fingerprint information
- **Responsive Design**: Mobile-optimized layout with adaptive columns
- **Real-Time Analytics**: Live statistics display with event tracking
- **Session Information**: Detailed browser, OS, screen, language, timezone data
- **Bot Score Display**: Actual calculated bot scores (not clamped values)

## Tech Stack

### Frontend
- **React (Vite)**: Modern, fast development framework
- **JavaScript**: ES6+ with modern features
- **CSS-in-JS**: Component-based styling
- **Responsive Design**: Mobile-first approach
- **FingerprintJS**: Advanced browser fingerprinting library

### Backend
- **Spring Boot**: Java-based REST API
- **MySQL**: Relational database for data storage
- **Maven**: Dependency management
- **Real-Time Processing**: Live bot score calculation

### Database
- **MySQL**: Stores fingerprints, sessions, events, and user data
- **Optimized Schema**: Efficient data relationships and indexing
- **Visit Tracking**: Session counting and analytics
- **Bot Detection**: Real-time score calculation and storage

## Project Structure

```
guest_fingerprint/
|
|-- backend/                     # Spring Boot application
|   |-- src/
|   |   |-- main/
|   |   |   |-- java/
|   |   |   |   `-- com/example/guestfingerprint/
|   |   |   |       |-- Application.java    # Main application
|   |   |   |       |-- model/            # Data models
|   |   |   |       |-- repository/       # Data access layer
|   |   |   |       |-- controller/       # REST controllers
|   |   |   `-- resources/
|   |   |       |-- application.properties # Configuration
|   |   |       `-- schema.sql           # Database schema
|   |-- pom.xml                  # Maven dependencies
|
|-- frontend/                    # React frontend application
|   |-- src/
|   |   |-- components/         # Reusable React components
|   |   |-- utils/              # Utility functions
|   |   |   |-- App.jsx            # Main application
|   |   |   |-- DashboardSimple.js  # Analytics dashboard
|   |   |   |-- EnhancedApp.jsx    # Advanced fingerprinting
|   |   |-- package.json        # Dependencies
|   |-- public/                  # Static assets
|   |-- index.html              # Entry point
|
|-- README.md                   # This file
|-- database_schema.md           # Complete database schema documentation
```

## Key Components

### Frontend Components
- **EnhancedApp.jsx**: Advanced fingerprinting interface with session management
- **DashboardSimple.js**: Analytics dashboard with grid-based statistics
- **browserInfo.js**: Browser and system information detection
- **advancedFingerprint.js**: Core fingerprinting and bot detection logic

### Backend Services
- **Application.java**: Main Spring Boot application with REST endpoints
- **Session Management**: Create, update, and track user sessions
- **Event Tracking**: Log user interactions and system events
- **Bot Detection**: Real-time calculation and analysis

## Database Schema

The complete database schema is documented in `database_schema.md`, which includes:

### Core Tables
- **guest_session**: Session tracking with visit counting and bot detection
- **user**: Account management and stitching
- **event**: User interaction logging

### Key Features
- **Visit Counting**: Automatic increment on each session
- **Bot Scoring**: Real-time calculation from indicators
- **Account Linking**: Multiple sessions to single user
- **Mobile Optimization**: Responsive design and touch support

### Schema Documentation
- **Complete Schema**: See `database_schema.md` for detailed table structures
- **Setup Instructions**: SQL commands for database creation
- **Field Descriptions**: Comprehensive documentation of all table fields
- **Security Notes**: Guidelines for handling sensitive data

## API Endpoints

### Session Management
- `POST /api/sessions`: Create new session
- `GET /api/sessions`: Retrieve all sessions
- `POST /api/events`: Track user events

### User Management
- `POST /api/users/login`: User authentication and account stitching
- `GET /api/users`: Retrieve user information

### Analytics
- `GET /api/events/count`: Get event statistics
- `GET /api/events`: Retrieve recent events

## Bot Detection

### Indicators
- **Browser Analysis**: User agent and browser characteristics
- **Plugin Detection**: Available browser plugins count
- **Language Preferences**: Browser language settings
- **Screen Resolution**: Display capabilities and dimensions
- **Behavioral Analysis**: Interaction patterns and timing

### Scoring System
- **Real-Time Calculation**: Dynamic score based on indicators
- **Unclamped Values**: Actual scores displayed in UI
- **Database Storage**: Clamped values for YES/NO determination
- **Threshold Logic**: > 0.5 = YES, ≤ 0.5 = NO

## Mobile Features

### Responsive Design
- **Grid Adaptation**: 4 columns (desktop) → 2 columns (mobile)
- **Text Optimization**: High contrast for mobile readability
- **Touch Support**: Mobile-friendly interaction design
- **Layout Optimization**: Prevents overflow and boundary issues

### Performance
- **Lazy Loading**: Component-based loading strategy
- **Optimized Rendering**: Efficient DOM updates
- **Mobile Performance**: Reduced animations and transitions

## Installation and Setup

### Prerequisites
- **Java 17+**: Backend runtime requirement
- **Node.js 18+**: Frontend development environment
- **MySQL 8.0+**: Database requirement
- **Maven 3.6+**: Build tool requirement

### Backend Setup
```bash
# Clone repository
git clone <repository-url>
cd guest_fingerprint/backend

# Configure database
mysql -u root -p
CREATE DATABASE guest_fingerprint;
source src/main/resources/schema.sql;

# Run application
mvnw.cmd spring-boot:run
```

### Frontend Setup
```bash
# Navigate to frontend
cd ../frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

## Configuration

### Database Configuration
- **Connection**: MySQL database connection settings
- **Schema**: Automatic table creation on startup
- **Indexing**: Optimized for performance

### Application Settings
- **API Base URL**: Configurable for different environments
- **Bot Detection**: Adjustable sensitivity and thresholds
- **Session Timeout**: Configurable session lifetime

## Development

### Environment Variables
- **Database URL**: MySQL connection string
- **API Port**: Backend service port (default: 8080)
- **Frontend Port**: Development server port (default: 5173)

### Build Process
- **Backend**: Maven build with dependency management
- **Frontend**: Vite build with optimization
- **Production**: Optimized builds for deployment

## Security Features

### Data Protection
- **Fingerprint Hashing**: Secure storage of visitor identifiers
- **Session Encryption**: Protected session data transmission
- **Input Validation**: Sanitized user inputs and parameters

### Bot Detection
- **Advanced Algorithms**: Multiple detection techniques
- **Real-Time Analysis**: Immediate threat assessment
- **Configurable Thresholds**: Adjustable sensitivity levels

---

A sophisticated visitor analytics system that combines advanced browser fingerprinting with intelligent bot detection to provide comprehensive insights into user behavior while maintaining robust security and real-time performance.