package backend.config;

import backend.service.UserService;
import backend.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.HashMap;

@Component
public class WebSocketDisconnectHandler {

    @Autowired
    private UserService userService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String userId = (String) headerAccessor.getSessionAttributes().get("userId");
        
        System.out.println("🔌 WebSocket disconnected - Session: " + sessionId + ", User: " + userId);
        System.out.println("🔌 Session attributes: " + headerAccessor.getSessionAttributes());
        
        if (userId != null && !userId.isEmpty()) {
            try {
                // Only delete the specific user who disconnected
                User user = userService.getUserInfo(userId);
                if (user != null) {
                    // Delete this specific user immediately
                    userService.deleteUser(userId);
                    System.out.println("🗑️ Deleted user: " + userId);
                    
                    // Broadcast updated users list
                    var activeUsers = userService.findRecentlyActiveUsers(5);
                    Map<String, User> activeUsersWithInfo = new HashMap<>();
                    for (User activeUser : activeUsers) {
                        activeUsersWithInfo.put(activeUser.getUserId(), activeUser);
                    }
                    // Use messagingTemplate to broadcast
                    messagingTemplate.convertAndSend("/topic/users/doc1", activeUsersWithInfo);
                    System.out.println("📤 Broadcasted updated users list after deletion: " + activeUsersWithInfo.size() + " users");
                }
            } catch (Exception e) {
                System.err.println("⚠️ Error deleting user: " + e.getMessage());
            }
        }
    }
}
