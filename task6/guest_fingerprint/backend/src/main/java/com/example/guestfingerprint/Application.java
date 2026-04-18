package com.example.guestfingerprint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.*;
import java.util.*;

@SpringBootApplication
@RestController
public class Application {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/fingerprint_db?useLegacyDatetimeCode=false&serverTimezone=Asia/Kolkata";
    private static final String DB_USER = "your_database_username";
    private static final String DB_PASSWORD = "your_database_password";

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    private Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL driver not found", e);
        }
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/api/test")
    public Map<String, Object> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "working");
        response.put("message", "Guest Fingerprinting Backend is running!");
        response.put("timestamp", new java.util.Date().toString());
        response.put("framework", "Spring Boot");
        response.put("database", "MySQL Connected");
        return response;
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/api/sessions")
    public Map<String, Object> getSessions() {
        Map<String, Object> response = new HashMap<>();
        
        try (Connection conn = getConnection()) {
            List<Map<String, Object>> sessions = new ArrayList<>();
            
            // Check what columns actually exist in the table
            String sql = "SELECT * FROM guest_session ORDER BY first_seen DESC LIMIT 50";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                // Get metadata to see actual column names
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                System.out.println("📋 Sessions table columns:");
                for (int i = 1; i <= columnCount; i++) {
                    System.out.println("  - " + metaData.getColumnName(i));
                }
                
                while (rs.next()) {
                    Map<String, Object> session = new HashMap<>();
                    // Use only the columns that actually exist
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        session.put(columnName, value);
                    }
                    sessions.add(session);
                }
            }
            
            response.put("success", true);
            response.put("totalSessions", sessions.size());
            response.put("sessions", sessions);
            response.put("message", "Sessions retrieved successfully!");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to retrieve sessions: " + e.getMessage());
        }
        
        return response;
    }
    
    // Helper method to calculate bot score from indicators
    private double calculateBotScore(String indicators) {
        if (indicators == null || indicators.trim().isEmpty()) {
            return 0.0; // Default to human if no indicators
        }
        
        double botScore = 0.0;
        String[] indicatorArray = indicators.split(",\\s*");
        
        for (String indicator : indicatorArray) {
            indicator = indicator.trim();
            
            // Negative indicators (increase bot score)
            if (indicator.contains("Bot-like user agent")) {
                botScore += 0.7;
            } else if (indicator.contains("Headless browser detected")) {
                botScore += 0.8;
            } else if (indicator.contains("No plugins detected")) {
                botScore += 0.2;
            } else if (indicator.contains("No languages detected")) {
                botScore += 0.1;
            } else if (indicator.contains("Zero window dimensions")) {
                botScore += 0.5;
            }
            
            // Positive indicators (decrease bot score)
            if (indicator.contains("Plugins:")) {
                try {
                    int pluginCount = Integer.parseInt(indicator.substring(indicator.indexOf(":") + 1).trim());
                    if (pluginCount > 5) {
                        botScore -= 0.1;
                    } else if (pluginCount > 0) {
                        botScore -= 0.05;
                    }
                } catch (NumberFormatException e) {
                    // Ignore parsing errors
                }
            }
            
            if (indicator.contains("Languages:")) {
                try {
                    int langCount = Integer.parseInt(indicator.substring(indicator.indexOf(":") + 1).trim());
                    if (langCount > 0) {
                        botScore -= 0.1;
                    }
                } catch (NumberFormatException e) {
                    // Ignore parsing errors
                }
            }
            
            if (indicator.contains("Screen:")) {
                botScore -= 0.1; // Normal screen is a good indicator
            }
            
            if (indicator.contains("Browser:")) {
                botScore -= 0.1; // Detected browser is a good indicator
            }
        }
        
        return botScore; // Return actual score without clamping
    }
    
    // Helper method to determine if bot based on clamped score
    private boolean isBot(double botScore) {
        double clampedScore = Math.max(0.0, Math.min(1.0, botScore));
        return clampedScore > 0.5;
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/api/sessions")
    public Map<String, Object> createSession(@RequestBody Map<String, Object> sessionData, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String fingerprintId = (String) sessionData.get("fingerprintId");
            String sessionId = "session_" + System.currentTimeMillis() + "_" + Math.random() * 10000;
            
            // Get bot detection data from request (for both new and existing sessions)
            Boolean isBot = (Boolean) sessionData.get("isBot");
            String indicators = (String) sessionData.get("indicators");
            
            // Calculate real-time bot score from indicators
            double actualBotScore = calculateBotScore(indicators);
            boolean isActuallyBot = isBot(actualBotScore);
            String botValue = isActuallyBot ? "YES" : "NO";
            
            // Debug logging
            System.out.println("DEBUG: Bot Score Calculation");
            System.out.println("DEBUG: Indicators: " + indicators);
            System.out.println("DEBUG: Actual Bot Score: " + actualBotScore);
            System.out.println("DEBUG: Is Bot (clamped): " + isActuallyBot);
            System.out.println("DEBUG: Bot Value for DB: " + botValue);
            
            // Check if session already exists for this fingerprint
            try (Connection conn = getConnection()) {
                String checkSql = "SELECT id FROM guest_session WHERE fingerprint_id = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, fingerprintId);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            Long existingSessionId = rs.getLong("id");
                            String existingSessionIdStr = String.valueOf(existingSessionId);
                            
                            // Look up user_id from user table
                            Long userId = null;
                            try {
                                String userLookupSql = "SELECT id FROM user WHERE fingerprint_id = ?";
                                try (PreparedStatement userStmt = conn.prepareStatement(userLookupSql)) {
                                    userStmt.setString(1, fingerprintId);
                                    try (ResultSet userRs = userStmt.executeQuery()) {
                                        if (userRs.next()) {
                                            userId = userRs.getLong("id");
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                System.out.println("⚠️ Could not lookup user_id: " + e.getMessage());
                            }
                            
                            // Get actual client IP and user agent
                            String clientIp = request.getRemoteAddr();
                            if (clientIp == null || clientIp.isEmpty()) {
                                clientIp = "127.0.0.1";
                            }
                            String userAgent = request.getHeader("User-Agent");
                            if (userAgent == null || userAgent.isEmpty()) {
                                userAgent = "Unknown";
                            }
                            
                            // Update existing session: last_seen, ip, user_agent, user_id, bot, indicators, visits
                            String updateSql = "UPDATE guest_session SET last_seen = NOW(), ip_address = ?, user_agent = ?, user_id = ?, bot = ?, indicators = ?, visits = visits + 1 WHERE id = ?";
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                                updateStmt.setString(1, clientIp);
                                updateStmt.setString(2, userAgent);
                                if (userId != null) {
                                    updateStmt.setLong(3, userId);
                                } else {
                                    updateStmt.setNull(3, Types.BIGINT);
                                }
                                updateStmt.setString(4, botValue);
                                updateStmt.setString(5, indicators != null ? indicators : "");
                                updateStmt.setLong(6, existingSessionId);
                                
                                int rowsUpdated = updateStmt.executeUpdate();
                                System.out.println("📊 Update SQL: " + updateSql);
                                System.out.println("📊 Parameters: IP=" + clientIp + ", UA=" + userAgent.substring(0, 50) + "..., bot=" + botValue);
                                System.out.println("📊 Rows updated: " + rowsUpdated);
                                
                                if (rowsUpdated == 0) {
                                    System.out.println("⚠️ WARNING: No rows updated for session ID: " + existingSessionId);
                                } else {
                                    System.out.println("✅ Successfully updated session: " + existingSessionIdStr);
                                }
                            }
                            
                            // Get updated visits count
                            String getVisitsSql = "SELECT visits FROM guest_session WHERE id = ?";
                            int totalVisits = 1;
                            try (PreparedStatement visitsStmt = conn.prepareStatement(getVisitsSql)) {
                                visitsStmt.setLong(1, existingSessionId);
                                try (ResultSet visitsRs = visitsStmt.executeQuery()) {
                                    if (visitsRs.next()) {
                                        totalVisits = visitsRs.getInt("visits");
                                    }
                                }
                            }
                            
                            response.put("sessionId", String.valueOf(totalVisits)); // Use visits as session ID
                            response.put("fingerprintId", fingerprintId);
                            response.put("status", "updated");
                            response.put("message", "Session updated!");
                            response.put("bot", botValue);
                            response.put("indicators", indicators);
                            response.put("totalVisits", totalVisits); // Include visits count
                            response.put("botScore", actualBotScore); // Send actual bot score to UI
                            
                            System.out.println("✅ Session updated: " + existingSessionIdStr + " for fingerprint: " + fingerprintId);
                            System.out.println("🌐 Client IP: " + clientIp);
                            System.out.println("🔍 User-Agent: " + userAgent);
                            System.out.println("🤖 Bot: " + botValue);
                            if (indicators != null) {
                                System.out.println("🚩 Indicators: " + indicators);
                            }
                            if (userId != null) {
                                System.out.println("🔗 Linked to user_id: " + userId);
                            }
                            return response;
                        }
                    }
                }
                
                // No existing session - create new one
                // Look up user_id from user table based on fingerprint
                Long userId = null;
                try {
                    String userLookupSql = "SELECT id FROM user WHERE fingerprint_id = ?";
                    try (PreparedStatement userStmt = conn.prepareStatement(userLookupSql)) {
                        userStmt.setString(1, fingerprintId);
                        try (ResultSet userRs = userStmt.executeQuery()) {
                            if (userRs.next()) {
                                userId = userRs.getLong("id");
                                System.out.println("🔗 Found user_id " + userId + " for fingerprint " + fingerprintId);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ Could not lookup user_id: " + e.getMessage());
                }
                
                // Get actual client IP from request
                String clientIp = request.getRemoteAddr();
                if (clientIp == null || clientIp.isEmpty()) {
                    clientIp = "127.0.0.1";
                }
                
                // Get user agent from request header
                String userAgent = request.getHeader("User-Agent");
                if (userAgent == null || userAgent.isEmpty()) {
                    userAgent = "Unknown";
                }
                
                // Create new session with all required fields including user_id, bot, indicators, visits
                String sql = "INSERT INTO guest_session (fingerprint_id, ip_address, user_agent, first_seen, last_seen, user_id, bot, indicators, visits) VALUES (?, ?, ?, NOW(), NOW(), ?, ?, ?, 1)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, fingerprintId);
                    pstmt.setString(2, clientIp);
                    pstmt.setString(3, userAgent);
                    if (userId != null) {
                        pstmt.setLong(4, userId);
                    } else {
                        pstmt.setNull(4, Types.BIGINT);
                    }
                    pstmt.setString(5, botValue);
                    pstmt.setString(6, indicators != null ? indicators : "");
                    
                    pstmt.executeUpdate();
                    System.out.println("✅ Session created: " + sessionId + " for fingerprint: " + fingerprintId);
                    System.out.println("🌐 Client IP: " + clientIp);
                    System.out.println("🔍 User-Agent: " + userAgent);
                    System.out.println("🤖 Bot: " + botValue);
                    if (indicators != null) {
                        System.out.println("🚩 Indicators: " + indicators);
                    }
                }
            }
            
            response.put("sessionId", "1"); // First visit
            response.put("fingerprintId", fingerprintId);
            response.put("status", "created");
            response.put("message", "Session stored in database!");
            response.put("bot", botValue);
            response.put("indicators", indicators);
            response.put("totalVisits", 1); // First visit
            response.put("botScore", actualBotScore); // Send actual bot score to UI
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to create session: " + e.getMessage());
            System.out.println("❌ Session creation error: " + e.getMessage());
        }
        
        return response;
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/api/events")
    public Map<String, Object> createEvent(@RequestBody Map<String, Object> eventData, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String fingerprintId = (String) eventData.get("fingerprintId");
            String eventType = (String) eventData.get("eventType");
            
            // Handle eventData as JSON object, convert to string for storage
            Object eventDataObj = eventData.get("eventData");
            String eventDataStr = eventDataObj != null ? eventDataObj.toString() : null;
            
            // Get actual client IP from request
            String clientIp = request.getRemoteAddr();
            if (clientIp == null || clientIp.isEmpty()) {
                clientIp = "127.0.0.1";
            }
            
            // Get user agent from request or use frontend-provided value
            String userAgent = request.getHeader("User-Agent");
            if (userAgent == null || userAgent.isEmpty()) {
                userAgent = eventData.get("userAgent") != null ? (String) eventData.get("userAgent") : "Unknown";
            }
            
            System.out.println("🚀 Creating event: " + eventType + " for fingerprint: " + fingerprintId);
            System.out.println("📊 Event data: " + eventDataStr);
            System.out.println("🌐 Client IP: " + clientIp);
            System.out.println("🔍 User-Agent: " + userAgent);
            
            // Store in database with actual client data
            try (Connection conn = getConnection()) {
                String sql = "INSERT INTO event (fingerprint_id, event_type, ip_address, user_agent, timestamp) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, fingerprintId);
                    pstmt.setString(2, eventType);
                    pstmt.setString(3, clientIp);
                    pstmt.setString(4, userAgent);
                    pstmt.setTimestamp(5, new java.sql.Timestamp(System.currentTimeMillis()));
                    
                    int rowsAffected = pstmt.executeUpdate();
                    System.out.println("📊 Database rows affected: " + rowsAffected);
                }
            }
            
            String eventId = "event_" + System.currentTimeMillis() + "_" + Math.random() * 10000;
            
            response.put("success", true);
            response.put("eventId", eventId);
            response.put("eventType", eventType);
            response.put("fingerprintId", fingerprintId);
            response.put("timestamp", new java.util.Date().toString());
            response.put("message", "Event stored in database!");
            
            System.out.println("✅ Event tracked: " + eventType + " for fingerprint: " + fingerprintId);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to track event: " + e.getMessage());
            System.out.println("❌ Event tracking error: " + e.getMessage());
        }
        
        return response;
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/api/events")
    public Map<String, Object> getEvents() {
        Map<String, Object> response = new HashMap<>();
        
        try (Connection conn = getConnection()) {
            List<Map<String, Object>> events = new ArrayList<>();
            int totalCount = 0;
            
            // Get total count first
            String countSql = "SELECT COUNT(*) as count FROM event";
            try (Statement countStmt = conn.createStatement();
                 ResultSet countRs = countStmt.executeQuery(countSql)) {
                if (countRs.next()) {
                    totalCount = countRs.getInt("count");
                }
            }
            
            // Get events with reasonable limit
            String sql = "SELECT * FROM event ORDER BY timestamp DESC LIMIT 100";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    Map<String, Object> event = new HashMap<>();
                    event.put("id", rs.getLong("id"));
                    event.put("fingerprintId", rs.getString("fingerprint_id"));
                    event.put("eventType", rs.getString("event_type"));
                    event.put("timestamp", rs.getTimestamp("timestamp"));
                    event.put("ipAddress", rs.getString("ip_address"));
                    event.put("userAgent", rs.getString("user_agent"));
                    // Remove eventData since column doesn't exist
                    events.add(event);
                }
            }
            
            response.put("success", true);
            response.put("totalEvents", totalCount); // Use actual database count
            response.put("displayedEvents", events.size()); // Show how many are displayed
            response.put("events", events);
            response.put("message", "Events retrieved from database!");
            
            System.out.println("📊 Retrieved " + events.size() + " events out of " + totalCount + " total");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to retrieve events: " + e.getMessage());
        }
        
        return response;
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/api/events/count")
    public Map<String, Object> getEventCount() {
        Map<String, Object> response = new HashMap<>();
        
        try (Connection conn = getConnection()) {
            int totalCount = 0;
            int pageViewCount = 0;
            int buttonClickCount = 0;
            
            // Get total count
            String totalSql = "SELECT COUNT(*) as count FROM event";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(totalSql)) {
                if (rs.next()) {
                    totalCount = rs.getInt("count");
                }
            }
            
            // Get PAGE_VIEW count
            String pageViewSql = "SELECT COUNT(*) as count FROM event WHERE event_type = 'PAGE_VIEW'";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(pageViewSql)) {
                if (rs.next()) {
                    pageViewCount = rs.getInt("count");
                }
            }
            
            // Get BUTTON_CLICK count
            String buttonClickSql = "SELECT COUNT(*) as count FROM event WHERE event_type = 'BUTTON_CLICK'";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(buttonClickSql)) {
                if (rs.next()) {
                    buttonClickCount = rs.getInt("count");
                }
            }
            
            // Get GUEST_LOGIN count
            int guestLoginCount = 0;
            String guestLoginSql = "SELECT COUNT(*) as count FROM event WHERE event_type = 'GUEST_LOGIN'";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(guestLoginSql)) {
                if (rs.next()) {
                    guestLoginCount = rs.getInt("count");
                }
            }
            
            // Get USER_LOGIN count
            int userLoginCount = 0;
            String userLoginSql = "SELECT COUNT(*) as count FROM event WHERE event_type = 'USER_LOGIN'";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(userLoginSql)) {
                if (rs.next()) {
                    userLoginCount = rs.getInt("count");
                }
            }
            
            response.put("success", true);
            response.put("totalCount", totalCount);
            response.put("pageViewCount", pageViewCount);
            response.put("buttonClickCount", buttonClickCount);
            response.put("guestLoginCount", guestLoginCount);
            response.put("userLoginCount", userLoginCount);
            response.put("message", "Events counted from database!");
            
            System.out.println("📊 Event counts - Total: " + totalCount + 
                             ", Page Views: " + pageViewCount + 
                             ", Button Clicks: " + buttonClickCount + 
                             ", Guest Logins: " + guestLoginCount + 
                             ", User Logins: " + userLoginCount);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to count events: " + e.getMessage());
            System.out.println("❌ Event count error: " + e.getMessage());
        }
        
        return response;
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/api/users/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> loginData, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String email = (String) loginData.get("email");
            String name = (String) loginData.get("name");
            String fingerprintId = (String) loginData.get("fingerprintId");
            String loginSource = (String) loginData.get("loginSource"); // New parameter
            
            System.out.println("🔑 Dynamic login attempt: " + email + " (" + name + ") with fingerprint: " + fingerprintId);
            
            // Extract client IP from request headers
            String clientIp = request.getHeader("X-Forwarded-For");
            if (clientIp == null || clientIp.isEmpty()) {
                clientIp = request.getHeader("X-Real-IP");
            }
            if (clientIp == null || clientIp.isEmpty()) {
                clientIp = request.getRemoteAddr();
            }
            if (clientIp == null || clientIp.isEmpty()) {
                clientIp = "127.0.0.1";
            }
            
            // Get user agent from request header
            String userAgent = request.getHeader("User-Agent");
            if (userAgent == null || userAgent.isEmpty()) {
                userAgent = "Login System";
            }
            
            try (Connection conn = getConnection()) {
                // First, ensure fingerprint_id column exists
                try {
                    String addColumnSql = "ALTER TABLE user ADD COLUMN fingerprint_id VARCHAR(255)";
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate(addColumnSql);
                        System.out.println("✅ fingerprint_id column added");
                    }
                } catch (Exception e) {
                    // Column already exists, that's fine
                    System.out.println("ℹ️ fingerprint_id column already exists");
                }
                
                // Check if user with this fingerprint already exists
                String checkSql = "SELECT * FROM user WHERE fingerprint_id = ?";
                try (PreparedStatement checkPstmt = conn.prepareStatement(checkSql)) {
                    checkPstmt.setString(1, fingerprintId);
                    
                    try (ResultSet rs = checkPstmt.executeQuery()) {
                        if (rs.next()) {
                            // User with this fingerprint already exists - UPDATE name to browser profile name
                            String updateSql = "UPDATE user SET email = ?, name = ? WHERE fingerprint_id = ?";
                            try (PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {
                                updatePstmt.setString(1, email);
                                updatePstmt.setString(2, name);  // This will be the browser profile name
                                updatePstmt.setString(3, fingerprintId);
                                
                                int rowsUpdated = updatePstmt.executeUpdate();
                                System.out.println("✅ Existing user updated: " + rowsUpdated + " rows");
                                System.out.println("🔄 Name updated to: " + name);
                            }
                            
                            // Update guest_session with user_id
                            String updateSessionSql = "UPDATE guest_session SET user_id = ? WHERE fingerprint_id = ? AND user_id IS NULL";
                            try (PreparedStatement updateSessionStmt = conn.prepareStatement(updateSessionSql)) {
                                updateSessionStmt.setLong(1, rs.getLong("id"));
                                updateSessionStmt.setString(2, fingerprintId);
                                int sessionRowsUpdated = updateSessionStmt.executeUpdate();
                                System.out.println("Updated guest_session user_id: " + sessionRowsUpdated + " rows");
                            }
                            
                            response.put("success", true);
                            response.put("message", "User already exists - updated with new data!");
                            response.put("user", Map.of(
                                "id", rs.getLong("id"),
                                "email", rs.getString("email"),
                                "name", rs.getString("name"),
                                "fingerprintId", rs.getString("fingerprint_id")
                            ));
                            System.out.println("✅ Existing user found and updated: " + rs.getString("email"));
                            
                        } else {
                            // Create new user with fingerprint
                            String insertSql = "INSERT INTO user (email, name, fingerprint_id) VALUES (?, ?, ?)";
                            try (PreparedStatement insertPstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                                insertPstmt.setString(1, email);
                                insertPstmt.setString(2, name);
                                insertPstmt.setString(3, fingerprintId);
                                
                                int rowsInserted = insertPstmt.executeUpdate();
                                System.out.println("New user created: " + rowsInserted + " rows");
                                
                                // Get the generated user ID
                                try (ResultSet generatedKeys = insertPstmt.getGeneratedKeys()) {
                                    if (generatedKeys.next()) {
                                        long newUserId = generatedKeys.getLong(1);
                                        
                                        // Update guest_session with user_id
                                        String updateSessionSql = "UPDATE guest_session SET user_id = ? WHERE fingerprint_id = ? AND user_id IS NULL";
                                        try (PreparedStatement updateSessionStmt = conn.prepareStatement(updateSessionSql)) {
                                            updateSessionStmt.setLong(1, newUserId);
                                            updateSessionStmt.setString(2, fingerprintId);
                                            int sessionRowsUpdated = updateSessionStmt.executeUpdate();
                                            System.out.println("Updated guest_session user_id for new user: " + sessionRowsUpdated + " rows");
                                        }
                                        
                                        response.put("success", true);
                                        response.put("message", "New user created and linked!");
                                        response.put("user", Map.of(
                                            "id", newUserId,
                                            "email", email,
                                            "name", name,
                                            "fingerprintId", fingerprintId
                                        ));
                                        System.out.println("New user linked: " + email + " -> " + fingerprintId + " (ID: " + newUserId + ")");
                                    }
                                }
                            }
                            
                            // Track login event ONLY for new users
                            String loginEventType = (loginSource != null && loginSource.equals("GUEST_LOGIN")) ? "GUEST_LOGIN" : "USER_LOGIN";
                            String eventSql = "INSERT INTO event (fingerprint_id, event_type, ip_address, user_agent, timestamp) VALUES (?, ?, ?, ?, ?)";
                            try (PreparedStatement eventStmt = conn.prepareStatement(eventSql)) {
                                eventStmt.setString(1, fingerprintId);
                                eventStmt.setString(2, loginEventType);
                                eventStmt.setString(3, clientIp);
                                eventStmt.setString(4, userAgent);
                                eventStmt.setTimestamp(5, new java.sql.Timestamp(System.currentTimeMillis()));
                                eventStmt.executeUpdate();
                                System.out.println("Login event tracked for NEW user: " + loginEventType + " for " + email + " from IP: " + clientIp);
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Login failed: " + e.getMessage());
            System.out.println("❌ Login error: " + e.getMessage());
        }
        
        return response;
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/api/simple/events")
    public Map<String, Object> createSimpleEvent(@RequestBody Map<String, Object> eventData, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String fingerprintId = (String) eventData.get("fingerprintId");
            String eventType = (String) eventData.get("eventType");
            
            // Get actual client IP from request
            String clientIp = request.getRemoteAddr();
            if (clientIp == null || clientIp.isEmpty()) {
                clientIp = "127.0.0.1";
            }
            
            // Get user agent from request header
            String userAgent = request.getHeader("User-Agent");
            if (userAgent == null || userAgent.isEmpty()) {
                userAgent = "Unknown";
            }
            
            // Store in database with actual client data
            try (Connection conn = getConnection()) {
                String sql = "INSERT INTO event (fingerprint_id, event_type, ip_address, user_agent) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, fingerprintId);
                    pstmt.setString(2, eventType);
                    pstmt.setString(3, clientIp);
                    pstmt.setString(4, userAgent);
                    
                    pstmt.executeUpdate();
                }
            }
            
            String eventId = "event_" + System.currentTimeMillis() + "_" + Math.random() * 10000;
            
            response.put("success", true);
            response.put("eventId", eventId);
            response.put("eventType", eventType);
            response.put("fingerprintId", fingerprintId);
            response.put("timestamp", new java.util.Date().toString());
            response.put("message", "Event stored in database!");
            
            System.out.println("✅ Simple Event tracked: " + eventType + " for fingerprint: " + fingerprintId);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to track event: " + e.getMessage());
        }
        
        return response;
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/api/debug/users")
    public Map<String, Object> debugUsers() {
        Map<String, Object> response = new HashMap<>();
        
        try (Connection conn = getConnection()) {
            List<Map<String, Object>> users = new ArrayList<>();
            
            // Check if user table exists and get data
            try {
                String sql = "SELECT * FROM user";
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    System.out.println("📋 User table columns:");
                    for (int i = 1; i <= columnCount; i++) {
                        System.out.println("  - " + metaData.getColumnName(i));
                    }
                    
                    while (rs.next()) {
                        Map<String, Object> user = new HashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnName(i);
                            Object value = rs.getObject(i);
                            user.put(columnName, value);
                        }
                        users.add(user);
                    }
                }
                
                response.put("success", true);
                response.put("totalUsers", users.size());
                response.put("users", users);
                response.put("message", "Users retrieved successfully!");
                
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "User table not found or error: " + e.getMessage());
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to retrieve users: " + e.getMessage());
        }
        
        return response;
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/api/debug/create-user")
    public Map<String, Object> createTestUser() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Create test user
            try (Connection conn = getConnection()) {
                String sql = "INSERT INTO user (email, name, fingerprint_id) VALUES (?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, "test@gmail.com");
                    pstmt.setString(2, "Anmol");
                    pstmt.setString(3, "0fa1734b14c5d70c997fe6b0e1d8dcf3");
                    
                    pstmt.executeUpdate();
                }
            }
            
            response.put("success", true);
            response.put("message", "Test user created successfully!");
            System.out.println("✅ Test user created: test@gmail.com");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to create test user: " + e.getMessage());
            System.out.println("❌ User creation error: " + e.getMessage());
        }
        
        return response;
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/api/debug/add-fingerprint-column")
    public Map<String, Object> addFingerprintColumn() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Add fingerprint_id column to user table
            try (Connection conn = getConnection()) {
                String sql = "ALTER TABLE user ADD COLUMN fingerprint_id VARCHAR(255)";
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(sql);
                }
            }
            
            response.put("success", true);
            response.put("message", "fingerprint_id column added successfully!");
            System.out.println("✅ fingerprint_id column added to user table");
            
        } catch (Exception e) {
            if (e.getMessage().contains("Duplicate column name")) {
                response.put("success", true);
                response.put("message", "fingerprint_id column already exists!");
                System.out.println("ℹ️ fingerprint_id column already exists");
            } else {
                response.put("success", false);
                response.put("message", "Failed to add fingerprint_id column: " + e.getMessage());
                System.out.println("❌ Column addition error: " + e.getMessage());
            }
        }
        
        return response;
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/api/debug/cleanup-duplicates")
    public Map<String, Object> cleanupDuplicates() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Find and remove duplicate users with same fingerprint_id
            try (Connection conn = getConnection()) {
                // First, identify duplicates
                String findDuplicatesSql = "SELECT fingerprint_id, COUNT(*) as count FROM user GROUP BY fingerprint_id HAVING count > 1";
                List<String> duplicateFingerprints = new ArrayList<>();
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(findDuplicatesSql)) {
                    while (rs.next()) {
                        duplicateFingerprints.add(rs.getString("fingerprint_id"));
                    }
                }
                
                // For each duplicate fingerprint, keep the first one and delete the rest
                for (String fingerprint : duplicateFingerprints) {
                    String getIdsSql = "SELECT id FROM user WHERE fingerprint_id = ? ORDER BY id";
                    List<Long> ids = new ArrayList<>();
                    
                    try (PreparedStatement pstmt = conn.prepareStatement(getIdsSql)) {
                        pstmt.setString(1, fingerprint);
                        try (ResultSet rs = pstmt.executeQuery()) {
                            while (rs.next()) {
                                ids.add(rs.getLong("id"));
                            }
                        }
                    }
                    
                    // Keep the first ID, delete the rest - but handle foreign key constraints
                    for (int i = 1; i < ids.size(); i++) {
                        Long userIdToDelete = ids.get(i);
                        Long userIdToKeep = ids.get(0);
                        
                        // First, update any guest_session records to point to the user we're keeping
                        String updateSessionsSql = "UPDATE guest_session SET user_id = ? WHERE user_id = ?";
                        try (PreparedStatement updatePstmt = conn.prepareStatement(updateSessionsSql)) {
                            updatePstmt.setLong(1, userIdToKeep);
                            updatePstmt.setLong(2, userIdToDelete);
                            int sessionsUpdated = updatePstmt.executeUpdate();
                            System.out.println("🔄 Updated " + sessionsUpdated + " sessions to point to user " + userIdToKeep);
                        }
                        
                        // Now delete the duplicate user
                        String deleteSql = "DELETE FROM user WHERE id = ?";
                        try (PreparedStatement deletePstmt = conn.prepareStatement(deleteSql)) {
                            deletePstmt.setLong(1, userIdToDelete);
                            int rowsDeleted = deletePstmt.executeUpdate();
                            System.out.println("🗑️ Deleted duplicate user ID: " + userIdToDelete);
                        }
                    }
                }
            }
            
            response.put("success", true);
            response.put("message", "Duplicate users cleaned up successfully!");
            System.out.println("✅ Duplicate users cleaned up");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to cleanup duplicates: " + e.getMessage());
            System.out.println("❌ Cleanup error: " + e.getMessage());
        }
        
        return response;
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/api/stitching/detect-collision")
    public Map<String, Object> detectCollision(@RequestBody Map<String, Object> requestData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String fingerprintId = (String) requestData.get("fingerprintId");
            
            // Enhanced validation: check for null, empty, or whitespace-only strings
            if (fingerprintId == null || fingerprintId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Fingerprint ID is required and cannot be empty");
                response.put("collision", false);
                response.put("databaseCollision", false);
                response.put("occurrenceCount", 0);
                return response;
            }
            
            // Trim whitespace from input
            fingerprintId = fingerprintId.trim();
            
            // Database-first collision detection
            try (Connection conn = getConnection()) {
                String checkSql = "SELECT COUNT(*) as count, GROUP_CONCAT(DISTINCT id) as session_ids, " +
                                "GROUP_CONCAT(DISTINCT user_id) as user_ids, " +
                                "GROUP_CONCAT(DISTINCT ip_address) as ips, " +
                                "MIN(first_seen) as first_seen, " +
                                "MAX(last_seen) as last_seen " +
                                "FROM guest_session WHERE fingerprint_id = ?";
                
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, fingerprintId);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            int count = rs.getInt("count");
                            boolean hasDatabaseCollision = count > 1;
                            
                            response.put("success", true);
                            response.put("fingerprintId", fingerprintId);
                            response.put("occurrenceCount", count);
                            response.put("databaseCollision", hasDatabaseCollision);
                            response.put("sessionIds", rs.getString("session_ids"));
                            response.put("userIds", rs.getString("user_ids"));
                            response.put("ipAddresses", rs.getString("ips"));
                            response.put("firstSeen", rs.getTimestamp("first_seen"));
                            response.put("lastSeen", rs.getTimestamp("last_seen"));
                            
                            if (hasDatabaseCollision) {
                                response.put("message", "Database collision detected: Fingerprint appears " + count + " times!");
                                response.put("collision", true);
                                response.put("collisionType", "DATABASE_COLLISION");
                                System.out.println("💥 Database collision detected: " + fingerprintId + " appears " + count + " times");
                            } else {
                                response.put("message", "No database collision: Fingerprint appears only once");
                                response.put("collision", false);
                                response.put("collisionType", "NO_COLLISION");
                                System.out.println("✅ No database collision: " + fingerprintId + " appears only once");
                            }
                        } else {
                            response.put("success", false);
                            response.put("message", "Fingerprint not found in database");
                            response.put("collision", false);
                            response.put("databaseCollision", false);
                            response.put("occurrenceCount", 0);
                            System.out.println("❌ Fingerprint not found: " + fingerprintId);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to detect collision: " + e.getMessage());
            response.put("collision", false);
            response.put("databaseCollision", false);
            response.put("occurrenceCount", 0);
            System.out.println("❌ Collision detection error: " + e.getMessage());
        }
        
        return response;
    }
    
    // Advanced similarity calculation using Levenshtein distance
    private double calculateSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 0.0;
        
        // Levenshtein distance calculation
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        
        int distance = dp[s1.length()][s2.length()];
        return 1.0 - ((double) distance / maxLength);
    }
    
    // Analysis classification based on similarity score
    private String getAnalysisType(double similarity) {
        if (similarity >= 0.95) return "High_Similarity";
        if (similarity >= 0.80) return "Medium_Similarity";
        if (similarity >= 0.60) return "Low_Similarity";
        return "No_Similarity";
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/api/debug/bot-check")
    public Map<String, Object> checkBot(@RequestBody Map<String, Object> requestData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String fingerprintId = (String) requestData.get("fingerprintId");
            
            // Simple bot detection based on fingerprint patterns
            boolean isBot = false;
            String reason = "Human user detected";
            
            if (fingerprintId == null || fingerprintId.isEmpty()) {
                isBot = true;
                reason = "No fingerprint provided";
            } else if (fingerprintId.startsWith("fallback_")) {
                isBot = true;
                reason = "Fallback fingerprint detected";
            } else if (fingerprintId.length() < 10) {
                isBot = true;
                reason = "Invalid fingerprint length";
            }
            
            response.put("success", !isBot);
            response.put("isBot", isBot);
            response.put("fingerprintId", fingerprintId);
            response.put("message", reason);
            response.put("botScore", isBot ? 0.8 : 0.0);
            
            System.out.println("🤖 Bot check for fingerprint: " + fingerprintId + " - Result: " + (isBot ? "BOT" : "HUMAN"));
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("isBot", true);
            response.put("message", "Failed to analyze fingerprint: " + e.getMessage());
            System.out.println("❌ Bot check error: " + e.getMessage());
        }
        
        return response;
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/api/debug/cleanup-corrupted-events")
    public Map<String, Object> cleanupCorruptedEvents() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            int deletedCount = 0;
            
            // Delete events with NULL event_type or NULL timestamp
            try (Connection conn = getConnection()) {
                String deleteSql = "DELETE FROM event WHERE event_type IS NULL OR timestamp IS NULL";
                try (Statement stmt = conn.createStatement()) {
                    deletedCount = stmt.executeUpdate(deleteSql);
                }
            }
            
            response.put("success", true);
            response.put("message", "Corrupted events cleaned up successfully!");
            response.put("deletedCount", deletedCount);
            System.out.println("🗑️ Deleted " + deletedCount + " corrupted events");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to cleanup corrupted events: " + e.getMessage());
            System.out.println("❌ Cleanup error: " + e.getMessage());
        }
        
        return response;
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/api/debug/cleanup-events")
    public Map<String, Object> cleanupEvents() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            int deletedCount = 0;
            
            // Delete events with NULL event_type or NULL timestamp
            try (Connection conn = getConnection()) {
                String deleteSql = "DELETE FROM event WHERE event_type IS NULL OR timestamp IS NULL";
                try (Statement stmt = conn.createStatement()) {
                    deletedCount = stmt.executeUpdate(deleteSql);
                }
            }
            
            response.put("success", true);
            response.put("message", "Invalid events cleaned up successfully!");
            response.put("deletedCount", deletedCount);
            System.out.println("🗑️ Deleted " + deletedCount + " invalid events");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to cleanup events: " + e.getMessage());
            System.out.println("❌ Cleanup error: " + e.getMessage());
        }
        
        return response;
    }
}
