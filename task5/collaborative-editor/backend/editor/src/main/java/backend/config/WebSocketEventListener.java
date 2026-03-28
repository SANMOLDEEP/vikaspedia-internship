package backend.config;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String userId = (String) event.getMessage().getHeaders().get("simpUser");
        if (userId != null) {
            System.out.println("� WebSocket disconnected - User: " + userId);
        }
    }
}
