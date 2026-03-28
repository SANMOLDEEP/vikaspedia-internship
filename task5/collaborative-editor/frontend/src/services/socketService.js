import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";

let stompClient = null;
let isConnected = false;
let heartbeatInterval = null;
let lastHeartbeatResponse = Date.now();

export const connectSocket = (documentId, userId, onMessageReceived, onUsersReceived, onConnectionChange) => {
  console.log("🚀 connectSocket called with documentId:", documentId, "userId:", userId);
  
  // Force disconnect if already connected with different user
  if (stompClient && isConnected) {
    console.log("🔄 Disconnecting existing connection...");
    stompClient.deactivate();
    stompClient = null;
    isConnected = false;
  }

  // Detect if running on mobile
  const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
  const serverUrl = isMobile ? "http://192.168.31.67:8081/ws" : "http://localhost:8081/ws";
  
  console.log("📱 Device type:", isMobile ? "Mobile" : "Desktop");
  console.log("🌐 Connecting to:", serverUrl);

  console.log("🔧 Creating SockJS connection to:", serverUrl);
  const socket = new SockJS(serverUrl);
  
  console.log("🔧 Creating STOMP client...");
  stompClient = new Client({
    webSocketFactory: () => socket,
    reconnectDelay: isMobile ? 10000 : 5000, // Longer reconnect delay for mobile
    debug: (str) => console.log("[STOMP]", str),
    
    onConnect: () => {
      console.log("✅ STOMP Connected as:", userId);
      console.log("📱 Connected from:", isMobile ? "Mobile" : "Desktop");
      console.log("🔍 Setting up subscriptions...");
      isConnected = true;
      lastHeartbeatResponse = Date.now();

      // Start heartbeat for all users (both mobile and desktop)
      heartbeatInterval = setInterval(() => {
        if (stompClient && isConnected) {
          console.log("💓 Sending heartbeat to stay active...");
          stompClient.publish({
            destination: `/app/ping/${documentId}`,
            body: JSON.stringify({ userId, timestamp: Date.now(), type: 'heartbeat' }),
          });
        }
      }, 30000); // Send heartbeat every 30 seconds for all users

      // Subscribe to document topic
      console.log("🔍 Subscribing to /topic/document/" + documentId);
      stompClient.subscribe(`/topic/document/${documentId}`, (msg) => {
        console.log("📩 Document message received:", msg.body);
        const data = JSON.parse(msg.body);
        console.log("🔍 Message from user:", data.userId, "Current user:", userId);
        console.log("🔍 Should process message:", data.userId !== userId);
        console.log("🔍 Message operation:", data.operation);
        console.log("🔍 Message text length:", data.text ? data.text.length : 0);
        
        // Process content operations from all users (including self) since it's database-verified content
        // Only ignore other operations from self
        if (data.operation === "content") {
          console.log("📤 Processing content operation (including self):", data.userId);
          onMessageReceived(data);
        } else if (data.userId !== userId) {
          console.log("📤 Processing remote message from:", data.userId);
          onMessageReceived(data);
        } else {
          console.log("🔇 Ignoring own message from:", data.userId, "operation:", data.operation);
        }
      });
      console.log("✅ Subscribed to document topic");

      // Subscribe to users topic
      stompClient.subscribe(`/topic/users/${documentId}`, (msg) => {
        console.log("📥 Users update received:", msg.body);
        const usersList = JSON.parse(msg.body);
        onUsersReceived(usersList);
      });

      // JOIN will be sent by Editor component after authentication
      console.log("📤 Connection established, waiting for Editor to send JOIN");
      
      // Notify connection status change
      if (onConnectionChange) {
        onConnectionChange(true);
      }
    },
    
    onDisconnect: () => {
      console.log("❌ STOMP Disconnected");
      isConnected = false;
      if (heartbeatInterval) {
        clearInterval(heartbeatInterval);
        heartbeatInterval = null;
      }
      
      // Notify connection status change
      if (onConnectionChange) {
        onConnectionChange(false);
      }
    },
    
    onError: (error) => {
      console.error("❌ STOMP Connection error:", error);
      console.error("❌ Error details:", error);
      isConnected = false;
    }
  });

  console.log("🚀 Activating STOMP client...");
  stompClient.activate();
};

export const sendMessage = (message) => {
  if (stompClient && isConnected) {
    let destination;
    
    if (message.operation === "JOIN" || message.operation === "join") {
      destination = `/app/join/${message.documentId}`;
      console.log("📤 Routing JOIN message to:", destination);
      console.log("📤 JOIN message details:", message);
    } else if (message.operation === "LEAVE") {
      destination = `/app/leave/${message.documentId}`;
    } else if (message.operation === "PING") {
      destination = `/app/ping/${message.documentId}`;
    } else if (message.operation === "REQUEST_CONTENT") {
      destination = `/app/request/${message.documentId}`;
    } else {
      destination = `/app/edit/${message.documentId}`;
    }
    
    console.log("📤 Sending to:", destination, "message:", message);
    
    stompClient.publish({
      destination: destination,
      body: JSON.stringify(message),
    });
  } else {
    console.warn("⚠️ Not connected yet");
  }
};

// JOIN is now handled by Editor component with authentication data

export const leaveDocument = (documentId, userId) => {
  console.log("📤 Attempting to leave document:", documentId, "user:", userId);
  
  if (stompClient && isConnected) {
    console.log("📤 Sending LEAVE message to backend");
    stompClient.publish({
      destination: `/app/leave/${documentId}`,
      body: JSON.stringify({ userId }),
    });
  } else {
    console.warn("⚠️ Cannot send LEAVE - not connected");
  }
};

export const disconnectSocket = () => {
  console.log("🔌 Force disconnecting socket...");
  
  if (heartbeatInterval) {
    clearInterval(heartbeatInterval);
    heartbeatInterval = null;
  }
  
  if (stompClient) {
    stompClient.deactivate();
    stompClient = null;
    isConnected = false;
  }
};

export const sendLeave = (documentId, userId) => {
  sendMessage({
    documentId,
    userId,
    operation: "LEAVE",
    text: "",
  });
};
