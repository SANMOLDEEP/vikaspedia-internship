package backend.controller;

import backend.dto.EditMessage;
import backend.service.DocumentService;
import backend.service.UserService;
import backend.service.CRDTService;
import backend.service.MultiDocumentService;
import backend.service.UserHistoryService;
import backend.model.User;
import backend.model.InsertOperation;
import backend.model.DeleteOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final DocumentService documentService;
    private final MultiDocumentService multiDocumentService;
    @Autowired
    private UserService userService;
    @Autowired
    private CRDTService crdtService;
    @Autowired
    private UserHistoryService userHistoryService;
    // Cache for user info to reduce database calls
    private final Map<String, User> userCache = new HashMap<>();

    @Autowired
    public WebSocketController(
            SimpMessagingTemplate messagingTemplate,
                               DocumentService documentService,
                               MultiDocumentService multiDocumentService,
                               UserService userService,
                               CRDTService crdtService,
                               UserHistoryService userHistoryService) {
        this.messagingTemplate = messagingTemplate;
        this.documentService = documentService;
        this.multiDocumentService = multiDocumentService;
        this.userService = userService;
        this.crdtService = crdtService;
        this.userHistoryService = userHistoryService;
    }

    @MessageMapping("/edit/{documentId}")
    public void handleEdit(
            @DestinationVariable String documentId,
            EditMessage message
    ) {
        try {
            System.out.println("📥 === EDIT MESSAGE DEBUG ===");
            System.out.println("📥 Edit received from: " + message.getUserId());
            System.out.println("📥 Document ID: " + documentId);
            System.out.println("📥 Raw message text: '" + message.getText() + "'");
            System.out.println("📥 Message text null: " + (message.getText() == null));
            System.out.println("📥 Message text empty: " + (message.getText() != null && message.getText().isEmpty()));
            System.out.println("📥 Content length: " + (message.getText() != null ? message.getText().length() : 0));
            
            String contentToSave = message.getText();
            
            // Handle null content
            if (contentToSave == null) {
                System.out.println("⚠️ Content is null, using empty string");
                contentToSave = "";
            }
            
            System.out.println("🔧 Final content to save: '" + contentToSave + "'");
            System.out.println("🔧 Final content length: " + contentToSave.length());
            System.out.println("🔧 Final content trimmed: '" + contentToSave.trim() + "'");
            System.out.println("🔧 Final content trimmed length: " + contentToSave.trim().length());
            
                        
            // Record user activity in history
            userHistoryService.recordUserEdit(message.getUserId(), documentId);
            
            // Use CRDT method to save content
            if (contentToSave.trim().length() > 0) {
                System.out.println("💾 USING CRDT to save content to database");
                System.out.println("💾 Content length: " + contentToSave.length());
                System.out.println("💾 Content hash: " + contentToSave.hashCode());
                System.out.println("💾 User ID: " + message.getUserId());
                System.out.println("💾 Document ID: " + documentId);
                
                // Get current content before saving
                String currentContent = multiDocumentService.getDocumentContent(documentId);
                System.out.println("💾 Current content before save: '" + currentContent + "'");
                
                String crdtResult = crdtService.applyCRDTOperation(documentId, message.getUserId(), contentToSave);
                System.out.println("💾 CRDT saved content: '" + crdtResult + "'");
                System.out.println("💾 CRDT result length: " + crdtResult.length());
                
                // Save content history with new row
                multiDocumentService.saveDocumentContent(documentId, crdtResult, message.getUserId());
                
                // Get saved content from database to verify
                String savedContent = multiDocumentService.getDocumentContent(documentId);
                System.out.println("🔍 Verified content from database: '" + savedContent + "'");
                System.out.println("🔍 Verified content length: " + savedContent.length());
                
                // Only broadcast if content actually changed
                if (!savedContent.equals(currentContent)) {
                    EditMessage broadcast = new EditMessage();
                    broadcast.setDocumentId(documentId);
                    broadcast.setUserId(message.getUserId());
                    broadcast.setOperation("content");
                    broadcast.setText(savedContent);
                    
                    messagingTemplate.convertAndSend("/topic/document/" + documentId, broadcast);
                    System.out.println("📤 Broadcasted CRDT content to ALL users: " + documentId + ", content: '" + savedContent + "'");
                } else {
                    System.out.println("📤 SKIPPING broadcast - content unchanged");
                }
            } else {
                System.out.println("⚠️ SKIPPING empty content save to prevent database overwrite");
                System.out.println("📤 SKIPPING broadcast - content was empty");
            }
            
            // Update user activity
            User currentUser = userService.getUserInfo(message.getUserId());
            if (currentUser != null) {
                currentUser.updateLastActive();
                userService.saveUser(currentUser);
            }
            
            System.out.println("📥 === END EDIT MESSAGE DEBUG ===");
        } catch (Exception e) {
            System.err.println("❌ Error handling edit message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @MessageMapping("/cursor/{documentId}")
    public void handleCursorMessage(
            @DestinationVariable String documentId,
            EditMessage message
    ) {
        System.out.println("📍 Cursor position from: " + message.getUserId() + " at position: " + message.getCursorPosition());
        
        try {
            // Broadcast cursor position to all other users
            EditMessage cursorMessage = new EditMessage();
            cursorMessage.setDocumentId(documentId);
            cursorMessage.setUserId(message.getUserId());
            cursorMessage.setOperation("cursor");
            cursorMessage.setCursorPosition(message.getCursorPosition());
            cursorMessage.setTimestamp(message.getTimestamp());
            
            messagingTemplate.convertAndSend("/topic/document/" + documentId, cursorMessage);
            System.out.println("📤 Broadcasted cursor position from: " + message.getUserId());
            
        } catch (Exception e) {
            System.err.println("❌ Error processing cursor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @MessageMapping("/focus/{documentId}")
    public void handleFocusMessage(
            @DestinationVariable String documentId,
            EditMessage message
    ) {
        System.out.println("🎯 Focus event from: " + message.getUserId());
        
        try {
            // Broadcast focus event to all other users
            EditMessage focusMessage = new EditMessage();
            focusMessage.setDocumentId(documentId);
            focusMessage.setUserId(message.getUserId());
            focusMessage.setOperation("focus");
            focusMessage.setTimestamp(message.getTimestamp());
            
            messagingTemplate.convertAndSend("/topic/document/" + documentId, focusMessage);
            System.out.println("📤 Broadcasted focus event from: " + message.getUserId());
            
        } catch (Exception e) {
            System.err.println("❌ Error processing focus: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @MessageMapping("/blur/{documentId}")
    public void handleBlurMessage(
            @DestinationVariable String documentId,
            EditMessage message
    ) {
        System.out.println("👁️ Blur event from: " + message.getUserId());
        
        try {
            // Broadcast blur event to all other users
            EditMessage blurMessage = new EditMessage();
            blurMessage.setDocumentId(documentId);
            blurMessage.setUserId(message.getUserId());
            blurMessage.setOperation("blur");
            blurMessage.setTimestamp(message.getTimestamp());
            
            messagingTemplate.convertAndSend("/topic/document/" + documentId, blurMessage);
            System.out.println("📤 Broadcasted blur event from: " + message.getUserId());
            
        } catch (Exception e) {
            System.err.println("❌ Error processing blur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @MessageMapping("/request/{documentId}")
    public void handleRequestMessage(
            @DestinationVariable String documentId,
            EditMessage message
    ) {
        System.out.println("📥 Content request from: " + message.getUserId());
        
        try {
            // Fetch current document content from database
            String currentContent = multiDocumentService.getDocumentContent(documentId);
            System.out.println("📤 Fetched content for request: '" + currentContent + "'");
            
            // Send content back to requesting user with content operation
            EditMessage contentMessage = new EditMessage();
            contentMessage.setDocumentId(documentId);
            contentMessage.setUserId("system");
            contentMessage.setOperation("content"); // Use regular content operation
            contentMessage.setText(currentContent != null ? currentContent : "");
            contentMessage.setTargetUserId(message.getUserId()); // Specify target user
            
            System.out.println("🚨 BACKEND: Sending content response");
            System.out.println("🚨 BACKEND: Target user: " + message.getUserId());
            System.out.println("🚨 BACKEND: Content: '" + (currentContent != null ? currentContent : "") + "'");
            
            messagingTemplate.convertAndSend("/topic/document/" + documentId, contentMessage);
            System.out.println("📤 Sent content in response to request: '" + (currentContent != null ? currentContent : "") + "'");
            
        } catch (Exception e) {
            System.err.println("❌ Error handling content request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @MessageMapping("/join/{documentId}")
    public void handleJoinMessage(
            @DestinationVariable String documentId,
            EditMessage message
    ) {
        System.out.println("📥 User joining: " + message.getUserId());
        
        try {
            // Parse user name from message
            java.util.Map<String, Object> userData = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(message.getText(), java.util.Map.class);
            
            String displayName = (String) userData.get("displayName");
            String userColor = (String) userData.get("userColor");
            String email = (String) userData.get("email");
            
            if (displayName != null && !displayName.trim().isEmpty()) {
                // Create or update user
                User user = userService.getUser(message.getUserId()).orElse(null);
                if (user == null) {
                    user = new User();
                    user.setUserId(message.getUserId());
                    user.setIsGuest(true);
                }
                
                user.setDisplayName(displayName);
                user.setEmail(email);
                if (userColor != null && !userColor.trim().isEmpty()) {
                    user.setUserColor(userColor);
                } else {
                    user.setUserColor("#FF6B6B"); // Default color
                }
                
                user.updateLastActive();
                userService.saveUser(user);
                System.out.println("📝 Updated user activity: " + message.getUserId());
                
                // Get all recently active users (last 5 minutes)
                List<User> recentUsers = userService.findRecentlyActiveUsers(5);
                Map<String, User> activeUsersWithInfo = new HashMap<>();
                for (User activeUser : recentUsers) {
                    activeUsersWithInfo.put(activeUser.getUserId(), activeUser);
                }
                
                // Broadcast updated users list to all clients
                messagingTemplate.convertAndSend("/topic/users/" + documentId, activeUsersWithInfo);
                System.out.println("📤 Broadcasted " + activeUsersWithInfo.size() + " active users");
                
                // Don't send content automatically on join - let new tabs request it
                // This prevents overwriting content in existing tabs
                System.out.println("✅ User joined: " + message.getUserId() + " - waiting for content request");
                
                // Clean up inactive users more aggressively (older than 2 minutes)
                int cleanedUsers = userService.cleanupInactiveUsers(2);
                if (cleanedUsers > 0) {
                    System.out.println("🧹 Cleaned up " + cleanedUsers + " inactive users");
                    
                    // Re-broadcast updated users list
                    List<User> updatedUsers = userService.findRecentlyActiveUsers(5);
                    Map<String, User> updatedUsersWithInfo = new HashMap<>();
                    for (User activeUser : updatedUsers) {
                        updatedUsersWithInfo.put(activeUser.getUserId(), activeUser);
                    }
                    messagingTemplate.convertAndSend("/topic/users/" + documentId, updatedUsersWithInfo);
                    System.out.println("📤 Re-broadcasted " + updatedUsersWithInfo.size() + " active users after cleanup");
                }
            } else {
                System.err.println("⚠️ No display name provided for user: " + message.getUserId());
            }
            
            // Record user join in history
            userHistoryService.recordUserJoin(message.getUserId(), documentId);
            
        } catch (Exception e) {
            System.err.println("⚠️ Error parsing JOIN message: " + e.getMessage());
        }
    }

    /**
     * Periodic cleanup of inactive users
     * Runs every 2 minutes to remove users inactive for more than 5 minutes
     */
    @Scheduled(fixedRate = 120000) // 2 minutes = 120000 ms
    public void periodicUserCleanup() {
        try {
            int cleanedUsers = userService.cleanupInactiveUsers(5); // 5 minutes
            if (cleanedUsers > 0) {
                System.out.println("🧹 Periodic cleanup: removed " + cleanedUsers + " inactive users");
                
                // Broadcast updated users list
                List<User> activeUsers = userService.findRecentlyActiveUsers(5);
                Map<String, User> activeUsersWithInfo = new HashMap<>();
                for (User activeUser : activeUsers) {
                    activeUsersWithInfo.put(activeUser.getUserId(), activeUser);
                }
                // Note: This should be updated to support multiple documents properly
                // For now, we'll disable the broadcast to avoid interfering with multi-document support
                System.out.println("📤 Skipping user broadcast to avoid multi-document conflicts");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error in periodic cleanup: " + e.getMessage());
        }
    }

    /**
     * Manual cleanup endpoint for testing
     */
    @MessageMapping("/cleanup")
    public void manualCleanup() {
        try {
            int cleanedUsers = userService.cleanupInactiveUsers(0.5); // 30 seconds
            System.out.println("🧹 Manual cleanup: removed " + cleanedUsers + " inactive users");
            
            // Broadcast updated users list
            List<User> activeUsers = userService.findRecentlyActiveUsers(5);
            Map<String, User> activeUsersWithInfo = new HashMap<>();
            for (User activeUser : activeUsers) {
                activeUsersWithInfo.put(activeUser.getUserId(), activeUser);
            }
            messagingTemplate.convertAndSend("/topic/users/doc1", activeUsersWithInfo);
            System.out.println("📤 Broadcasted updated users list: " + activeUsersWithInfo.size() + " users");
        } catch (Exception e) {
            System.err.println("⚠️ Error in manual cleanup: " + e.getMessage());
        }
    }
}
