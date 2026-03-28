import React, { useState, useEffect, useRef, useMemo } from 'react';
import { connectSocket, leaveDocument, sendMessage } from '../../services/socketService';
import { textChangeToOperations, applyOperations, composeOperations, applyCompactOperation, optimizeOperations } from "../../services/operationService";
import CursorOverlay from "../CursorOverlay/CursorOverlay";
import './Editor.css';

const Editor = ({ onUsersUpdate, documentId, documentName }) => {
  const [content, setContent] = useState("");
  const [isRemoteUpdate, setIsRemoteUpdate] = useState(false);
  const [editorUserId, setEditorUserId] = useState("");
  const [isConnected, setIsConnected] = useState(false);
  const [hasReceivedInitialContent, setHasReceivedInitialContent] = useState(false);
  const [activeCursors, setActiveCursors] = useState(new Map()); // Track other users' cursors
  const [lastEditingUser, setLastEditingUser] = useState(null); // Track who last edited
  const quillRef = useRef(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  const sendTimeout = useRef(null);
  const activityTimeout = useRef(null);
  
  // Track if we've intentionally set initial content
  const [hasSetInitialContent, setHasSetInitialContent] = useState(false);
  
  // Track if we're handling a remote update to prevent localStorage conflicts
  const isHandlingRemoteUpdate = useRef(false);
    
  // Generate a consistent userId based on session/tab
  const userId = useMemo(() => {
    let storedUserId = sessionStorage.getItem('collaborativeUserId');
    if (!storedUserId) {
      storedUserId = "user" + Math.floor(Math.random() * 1000);
      sessionStorage.setItem('collaborativeUserId', storedUserId);
    }
    return storedUserId;
  }, []);

  // Update the editorUserId state
  useEffect(() => {
    setEditorUserId(userId);
  }, [userId]);

  // Start with empty content - will be loaded from database via WebSocket
  useEffect(() => {
    // Only initialize if we have a valid documentId
    if (!documentId) {
      console.log("🔧 [MOUNT] No documentId provided - skipping initialization");
      return;
    }
    
    console.log("🔧 [MOUNT] Starting with empty content - will load from database");
    console.log("🔧 [MOUNT] User ID: " + userId);
    console.log("🔧 [MOUNT] Document ID: " + documentId);
    console.log("🔧 [MOUNT] Editor User ID: " + editorUserId);
    
    // Set editorUserId if not already set
    if (!editorUserId && userId) {
      setEditorUserId(userId);
      console.log("🔧 [MOUNT] Set editorUserId to: " + userId);
    }
    
    // Set initial content but don't send it to database immediately
    // This prevents the empty content overwrite issue while allowing proper initialization
    const initialContent = '';
    setContent(initialContent);
    setHasSetInitialContent(true);
    console.log("🔧 [MOUNT] Set initial content for UI, waiting for database content");
  }, [userId, editorUserId, documentId]);

  // Listen for storage events from other tabs
  useEffect(() => {
    const handleStorageChange = (e) => {
      if (e.key === 'documentContent' && e.newValue !== null) {
        console.log("🔧 Storage change detected in other tab: '" + e.newValue + "'");
        console.log("🔧 Current content: '" + content + "'");
        
        // Don't handle storage events if we're already handling a remote update
        if (isHandlingRemoteUpdate.current) {
          console.log("🔧 Skipping storage event - already handling remote update");
          return;
        }
        
        setIsRemoteUpdate(true);
        setContent(e.newValue);
        
        // Update textarea content immediately but preserve cursor position
        if (quillRef.current) {
          const currentCursor = quillRef.current.selectionStart;
          quillRef.current.value = e.newValue;
          // Restore cursor to a safe position (not beyond content length)
          const restoredCursor = Math.min(currentCursor, e.newValue.length);
          quillRef.current.setSelectionRange(restoredCursor, restoredCursor);
          console.log("🔧 Updated content from storage event: '" + e.newValue + "'");
          console.log("📍 Restored cursor position to: " + restoredCursor);
        }
        
        setTimeout(() => {
          setIsRemoteUpdate(false);
        }, 100);
      }
    };

    window.addEventListener('storage', handleStorageChange);
    
    return () => {
      window.removeEventListener('storage', handleStorageChange);
    };
  }, []); // Remove content dependency to prevent infinite loops

  useEffect(() => {
    // Only connect if we have both documentId and userId
    if (!documentId || !editorUserId) {
      console.log("🔧 [WEBSOCKET] Not connecting - missing documentId or userId");
      return;
    }
    
    console.log("🔧 [WEBSOCKET] Connecting to WebSocket with document: " + documentId + ", user: " + editorUserId);
    
    connectSocket(documentId, editorUserId, (data) => {
      
      if (data.operation === "compact") {
        
        // Apply compact operation if metadata is available
        if (data.metadata && data.metadata.compactOperation) {
          try {
            const compactOperation = JSON.parse(data.metadata.compactOperation);
            
            setIsRemoteUpdate(true);
            
            // Apply the compact operation
            const result = applyCompactOperation(content, compactOperation, editorUserId);
            setContent(result.content);
            localStorage.setItem('documentContent', result.content);
            
            setIsRemoteUpdate(false);
          } catch (e) {
            setIsRemoteUpdate(false);
          }
        }
      } else if (data.operation === "operation") {
        
        if (data.operations && data.operations.length > 0) {
          setIsRemoteUpdate(true);
          
          // Apply operations
          const result = applyOperations(content, data.operations, editorUserId);
          setContent(result.content);
          localStorage.setItem('documentContent', result.content);
            
          setIsRemoteUpdate(false);
        }
      } else if (data.operation === "content") {
        
        // Handle regular content updates
        console.log("🚨 CONTENT OPERATION RECEIVED!");
        console.log("🚨 Content operation from: " + data.userId);
        console.log("🚨 Target user: " + data.targetUserId);
        console.log("🚨 Current user: " + editorUserId);
        console.log("🚨 Content: '" + data.text + "'");
        console.log("🚨 Current content: '" + content + "'");
        
        // Only process if no target user OR if this user is the target
        const shouldProcess = !data.targetUserId || data.targetUserId === editorUserId;
        console.log("🚨 Should process: " + shouldProcess);
        
        // Update content if different and should process
        if (shouldProcess && data.text !== content) {
          console.log("🚨 UPDATING CONTENT FROM DATABASE");
          console.log("🚨 Updating from '" + content + "' to '" + data.text + "'");
          setIsRemoteUpdate(true);
          isHandlingRemoteUpdate.current = true;
          
          // Store current cursor position before updating content
          const currentCursor = quillRef.current ? quillRef.current.selectionStart : 0;
          console.log("📍 Current cursor position before update: " + currentCursor);
          
          // Update content state
          setContent(data.text);
          
          // Don't update localStorage here - let user input handle localStorage sync
          // This prevents cursor position conflicts between tabs
          
          // Update textarea content directly without changing cursor position
          if (quillRef.current) {
            quillRef.current.value = data.text;
            // Restore cursor position to where it was before the update
            const restoredCursor = Math.min(currentCursor, data.text.length);
            quillRef.current.setSelectionRange(restoredCursor, restoredCursor);
            console.log("🚨 Set textarea content from content operation: '" + data.text + "'");
            console.log("📍 Restored cursor position to: " + restoredCursor);
          }
          
          setTimeout(() => {
            setIsRemoteUpdate(false);
            isHandlingRemoteUpdate.current = false;
          }, 50);
        } else if (!shouldProcess) {
          console.log("🚨 IGNORING CONTENT - NOT FOR THIS USER");
        } else {
          console.log("🚨 Content same, skipping update");
        }
      } else if (data.operation === "edit") {
        
        // Handle content updates (including from system on new user join)
        console.log("🔧 Received edit operation from: " + data.userId);
        console.log("🔧 Edit content: '" + data.text + "'");
        console.log("🔧 Current content: '" + content + "'");
        
        // Update content if different or if from system (new user join)
        if (data.text !== content || data.userId === "system") {
          console.log("🔧 Updating content from edit operation");
          
          setIsRemoteUpdate(true);
          isHandlingRemoteUpdate.current = true;
          
          // Store current cursor position before updating content
          const currentCursor = quillRef.current ? quillRef.current.selectionStart : 0;
          console.log("📍 Current cursor position before edit update: " + currentCursor);
          
          setContent(data.text);
          // Don't update localStorage here - let user input handle localStorage sync
          // This prevents cursor position conflicts between tabs
          
          // Update textarea content as well without changing cursor position
          if (quillRef.current) {
            quillRef.current.value = data.text;
            // Restore cursor position to where it was before the update
            const restoredCursor = Math.min(currentCursor, data.text.length);
            quillRef.current.setSelectionRange(restoredCursor, restoredCursor);
            console.log("🔧 Set textarea content from edit: '" + data.text + "'");
            console.log("📍 Restored cursor position to: " + restoredCursor);
          }
          
          // Track who last edited
          if (data.userId !== editorUserId) {
            setLastEditingUser(data.userId);
            console.log("👤 Another user (" + data.userId + ") is editing");
          }
          
          // Reset remote update flags
          setTimeout(() => {
            setIsRemoteUpdate(false);
            isHandlingRemoteUpdate.current = false;
          }, 50);
        }
      } else if (data.operation === "cursor") {
        
        // Handle cursor position updates from other users
        if (data.userId !== editorUserId) {
          console.log("📍 Received cursor position from " + data.userId + ": " + data.cursorPosition);
          
          setActiveCursors(prev => {
            const newCursors = new Map(prev);
            newCursors.set(data.userId, {
              position: data.cursorPosition,
              timestamp: data.timestamp,
              userId: data.userId
            });
            return newCursors;
          });
        }
      } else if (data.operation === "focus") {
        
        // Handle focus events from other users
        if (data.userId !== editorUserId) {
          console.log("🎯 User " + data.userId + " focused on editor");
          setLastEditingUser(data.userId);
        }
      } else if (data.operation === "blur") {
        
        // Handle blur events from other users
        if (data.userId !== editorUserId) {
          console.log("👁️ User " + data.userId + " blurred from editor");
          // Remove cursor when user blurs
          setActiveCursors(prev => {
            const newCursors = new Map(prev);
            newCursors.delete(data.userId);
            return newCursors;
          });
        }
      } else if (data.operation === "crdt") {
        
        // CRDT operations are commutative - accept updates from all users
        console.log("🔧 CRDT: Received update from user: " + data.userId);
        console.log("🔧 CRDT: New content: '" + data.text + "'");
        
        // Always update content for CRDT
        setIsRemoteUpdate(true);
        
        // Update content and localStorage
        setContent(data.text);
        localStorage.setItem('documentContent', data.text);
        
        // Update textarea content directly
        if (quillRef.current) {
          quillRef.current.value = data.text;
          console.log("🔧 CRDT: Updated textarea content: '" + data.text + "'");
        }
        
        setTimeout(() => {
          setIsRemoteUpdate(false);
        }, 50);
      }
    }, (usersList) => {
      onUsersUpdate(usersList);
    }, (connected) => {
      setIsConnected(connected);
    });

    return () => {
      if (documentId && editorUserId) {
        leaveDocument(documentId, editorUserId);
      }
    };
  }, [documentId, editorUserId, onUsersUpdate]);

  // Send initial messages only after connection is established and editorUserId is set
  useEffect(() => {
    if (isConnected && editorUserId && editorUserId !== '') {
      console.log("🔧 Connected, sending JOIN and REQUEST messages");
      console.log("🔧 Using User ID: " + editorUserId);
      
      // Send JOIN message with guest user info
      const joinMessage = {
        documentId,
        userId: editorUserId,
        operation: "JOIN",
        text: JSON.stringify({
          displayName: sessionStorage.getItem('userDisplayName') || 'Guest User',
          email: sessionStorage.getItem('userEmail') || '',
          isGuest: true
        })
      };
      sendMessage(joinMessage);

      // Request initial document content immediately
      console.log("🔧 Requesting database content on connection");
      sendMessage({
        documentId,
        userId: editorUserId,
        operation: "REQUEST_CONTENT",
        text: ""
      });
      
      // Also request content again after a short delay to ensure we get it
      setTimeout(() => {
        console.log("🔧 Requesting database content again (backup)");
        sendMessage({
          documentId,
          userId: editorUserId,
          operation: "REQUEST_CONTENT",
          text: ""
        });
      }, 500);
    }
  }, [isConnected, editorUserId, documentId]);

  const handleChange = (e) => {
    if (isRemoteUpdate) {
      setIsRemoteUpdate(false);
      return;
    }

        
    // ✅ SIMPLE TEXTAREA HANDLING
    const textareaContent = e.target.value;
    const cursorPosition = e.target.selectionStart;
    
    console.log("🔧 === IMMEDIATE CONTENT DEBUG ===");
    console.log("🔧 e.target.value: '" + textareaContent + "'");
    console.log("🔧 textareaContent: '" + textareaContent + "'");
    console.log("🔧 textareaContent.length: " + textareaContent.length);
    console.log("🔧 User ID: " + editorUserId);
    console.log("🔧 Document ID: " + documentId);
    console.log("🔧 Cursor position: " + cursorPosition);
    console.log("🔧 Current content state: '" + content + "'");
    console.log("🔧 === END IMMEDIATE CONTENT DEBUG ===");
    
    // Update content state
    setContent(textareaContent);
    
    // Update localStorage for tab synchronization (only if not remote update and content is meaningful)
    if (!isRemoteUpdate && textareaContent.trim() !== "") {
      localStorage.setItem('documentContent', textareaContent);
    }

    // Send content to backend (will be saved to database)
    if (sendTimeout.current) {
      clearTimeout(sendTimeout.current);
    }

    sendTimeout.current = setTimeout(() => {
      // Only send non-empty content to prevent overwriting database
      console.log("🔧 === CONTENT SAVE DEBUG ===");
      console.log("🔧 textareaContent: '" + textareaContent + "'");
      console.log("🔧 textareaContent.trim(): '" + textareaContent.trim() + "'");
      console.log("🔧 textareaContent.trim().length: " + textareaContent.trim().length);
      console.log("🔧 Has initial content: " + hasReceivedInitialContent);
      
      // Only send if content is not empty OR if we're just initializing
      if (textareaContent.trim().length > 0 || (hasReceivedInitialContent && textareaContent.trim() === "")) {
        console.log("🔧 SENDING content to backend");
        
        // Send content including cursor position and editing info
        const message = {
          documentId,
          userId: editorUserId,
          operation: "edit",
          text: textareaContent,
          cursorPosition: cursorPosition,
          isEditing: true,
          timestamp: Date.now()
        };
        
        console.log("🔧 Message to send:", message);
        console.log("🔧 About to call sendMessage with:", message);
        
        // Send immediately and also add backup direct save
        sendMessage(message);
        
        // Also try direct save as backup
        try {
          fetch('http://localhost:8081/api/test/save/' + documentId, {
            method: 'POST',
            headers: {
              'Content-Type': 'text/plain',
            },
            body: textareaContent
          }).then(response => response.text())
           .then(result => console.log("🔧 Direct save result:", result))
           .catch(error => console.error("🔧 Direct save error:", error));
        } catch (e) {
          console.error("🔧 Direct save exception:", e);
        }
        
        // Track that this user is currently editing
        setLastEditingUser(editorUserId);
      } else {
        console.log("🔧 SKIPPING empty content send to prevent database overwrite");
      }
      
      console.log("🔧 === END CONTENT SAVE DEBUG ===");
    }, 300);
  };

  const handleCursorMove = (e) => {
    if (isRemoteUpdate) return;
    
    const cursorPosition = e.target.selectionStart;
    
    // Send cursor position to other users
    sendMessage({
      documentId,
      userId: editorUserId,
      operation: "cursor",
      cursorPosition: cursorPosition,
      timestamp: Date.now()
    });
    
    console.log("📍 Cursor moved to position: " + cursorPosition);
  };

  const handleFocus = (e) => {
    // Send focus event to other users
    sendMessage({
      documentId,
      userId: editorUserId,
      operation: "focus",
      timestamp: Date.now()
    });
    
    console.log("🎯 User focused on editor");
  };

  const handleBlur = (e) => {
    // Send blur event to other users
    sendMessage({
      documentId,
      userId: editorUserId,
      operation: "blur",
      timestamp: Date.now()
    });
    
    console.log("👁️ User blurred from editor");
  };

  // Handle browser/tab close
  useEffect(() => {
    const handleBeforeUnload = (e) => {
      leaveDocument(documentId, editorUserId);
    };

    const handleStorageChange = (e) => {
      if (e.key === 'documentContent' && e.newValue) {
        setContent(e.newValue);
      }
    };

    window.addEventListener('beforeunload', handleBeforeUnload);
    window.addEventListener('storage', handleStorageChange);
    
    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
      window.removeEventListener('storage', handleStorageChange);
    };
  }, [documentId, editorUserId, content]);

  // Send heartbeat on user interaction (debounced to avoid spam)
  const handleUserActivity = () => {
    if (activityTimeout.current) {
      clearTimeout(activityTimeout.current);
    }
      
    activityTimeout.current = setTimeout(() => {
        sendMessage({
          documentId,
          userId: editorUserId,
          operation: "PING",
          text: JSON.stringify({ type: 'user_activity', timestamp: Date.now() }),
        });
      }, 5000); // Send activity heartbeat after 5 seconds of inactivity
    };

  // Define event handlers
  const handleBeforeUnload = (e) => {
    leaveDocument(documentId, editorUserId);
  };

  const handleStorageChange = (e) => {
    if (e.key === 'documentContent' && e.newValue) {
      setContent(e.newValue);
    }
  };

  const handleVisibilityChange = () => {
    if (document.visibilityState === 'visible') {
      // User returned to the tab
      console.log('👁️ User returned to tab');
    } else {
      // User left the tab
      console.log('👁️ User left tab');
    }
  };

  // Set up event listeners for user activity
  useEffect(() => {
    window.addEventListener('beforeunload', handleBeforeUnload);
    window.addEventListener('storage', handleStorageChange);
    window.addEventListener('visibilitychange', handleVisibilityChange);
    window.addEventListener('mousemove', handleUserActivity);
    window.addEventListener('click', handleUserActivity);
    window.addEventListener('keydown', handleUserActivity);
    
    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
      window.removeEventListener('storage', handleStorageChange);
      window.removeEventListener('visibilitychange', handleVisibilityChange);
      window.removeEventListener('mousemove', handleUserActivity);
      window.removeEventListener('click', handleUserActivity);
      window.removeEventListener('keydown', handleUserActivity);
    };
  }, [documentId, editorUserId, content]);

  // Auto-authenticate for testing (remove this in production)
  useEffect(() => {
    sessionStorage.setItem('isAuthenticated', 'true');
    setIsAuthenticated(true);
  }, []);

  const isMobile = window.innerWidth <= 768;
  
  return (
    <div style={{ 
      height: '100%', 
      display: 'flex', 
      flexDirection: 'column',
      overflow: 'hidden', // Prevent overflow at container level
      width: '100%',
      maxWidth: '100vw' // Ensure container doesn't exceed viewport width
    }}>
      {/* Show who's currently editing */}
      {lastEditingUser && lastEditingUser !== editorUserId && (
        <div style={{
          backgroundColor: '#e3f2fd',
          color: '#1976d2',
          padding: isMobile ? '12px 16px' : '8px 12px',
          fontSize: isMobile ? '16px' : '14px',
          borderBottom: '1px solid #bbdefb',
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          flexShrink: 0 // Prevent shrinking
        }}>
          <span style={{ 
            display: 'inline-block',
            width: isMobile ? '10px' : '8px',
            height: isMobile ? '10px' : '8px',
            backgroundColor: '#4caf50',
            borderRadius: '50%',
            animation: 'pulse 1.5s infinite'
          }}></span>
          User {lastEditingUser} is editing...
        </div>
      )}
      
            
      <div style={{ 
        flex: 1, 
        position: 'relative',
        overflow: 'hidden', // Prevent overflow
        width: '100%',
        minWidth: 0 // Allow flex item to shrink below content size
      }}>
        {/* ✅ SIMPLE TEXTAREA - NO FORMATTING */}
        <textarea
          ref={quillRef}
          value={content}
          onChange={handleChange}
          onClick={handleCursorMove}
          onKeyUp={handleCursorMove}
          onKeyDown={handleChange}
          onFocus={handleFocus}
          onBlur={handleBlur}
          onSelect={handleCursorMove}
          placeholder={documentName ? `Start editing ${documentName}...` : "Start editing..."}
          style={{
            width: '100%',
            height: '100%',
            maxWidth: '100%', // Ensure textarea doesn't exceed container
            minWidth: '0', // Allow textarea to shrink
            border: 'none',
            outline: 'none',
            resize: 'none',
            padding: isMobile ? '20px 16px' : '20px',
            fontSize: isMobile ? '18px' : '16px',
            lineHeight: isMobile ? '1.7' : '1.6',
            fontFamily: isMobile ? 'monospace, monospace' : 'monospace',
            backgroundColor: 'transparent',
            color: '#333',
            WebkitTapHighlightColor: 'transparent',
            WebkitUserSelect: 'text',
            userSelect: 'text',
            boxSizing: 'border-box', // Include padding in width calculation
            overflowX: 'hidden', // Prevent horizontal scrolling
            overflowY: 'auto', // Allow vertical scrolling
            wordWrap: 'break-word', // Break long words
            overflowWrap: 'break-word', // Additional word wrapping
            whiteSpace: 'pre-wrap' // Preserve whitespace but allow wrapping
          }}
        />
        
        {/* Render other users' cursors */}
        {Array.from(activeCursors.entries()).map(([userId, cursor]) => (
          <div
            key={userId}
            style={{
              position: 'absolute',
              left: cursor.position * 8 + 20, // Approximate character width
              top: 20,
              width: '2px',
              height: '20px',
              backgroundColor: cursor.color || '#ff0000',
              pointerEvents: 'none',
            }}
          />
        ))}
      </div>
    </div>
  );
};

export default Editor;
