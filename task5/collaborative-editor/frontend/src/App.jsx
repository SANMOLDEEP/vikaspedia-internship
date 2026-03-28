import React, { useState, useEffect } from "react";
import Editor from "./components/Editor/Editor";
import ActiveUsers from "./components/ActiveUsers/ActiveUsers";
import DocumentList from "./components/DocumentList/DocumentList";
import Navbar from "./components/Navbar";
import AuthModal from "./components/Auth/AuthModal";

const App = () => {
  const [users, setUsers] = useState([]);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [currentUser, setCurrentUser] = useState(null);
  const [showAuthModal, setShowAuthModal] = useState(false);
  const [showMobileUsers, setShowMobileUsers] = useState(false);
  const [currentDocumentId, setCurrentDocumentId] = useState(null);
  const [currentDocumentName, setCurrentDocumentName] = useState(null);
  const [isMobile, setIsMobile] = useState(false);

  // Detect mobile device
  useEffect(() => {
    const checkMobile = () => {
      setIsMobile(window.innerWidth <= 768);
    };
    
    checkMobile();
    window.addEventListener('resize', checkMobile);
    
    return () => window.removeEventListener('resize', checkMobile);
  }, []);

  // Check if user is already authenticated on mount
  useEffect(() => {
    const authStatus = sessionStorage.getItem('isAuthenticated');
    const displayName = sessionStorage.getItem('userDisplayName');
    
    if (authStatus === 'true' && displayName) {
      setIsAuthenticated(true);
      setCurrentUser({
        displayName: displayName,
        email: sessionStorage.getItem('userEmail'),
        avatarUrl: sessionStorage.getItem('userAvatar')
      });
    } else {
      // Show auth modal if not authenticated
      setShowAuthModal(true);
    }
  }, []);

  const handleAuthComplete = (userData) => {
    setIsAuthenticated(true);
    setCurrentUser(userData);
    setShowAuthModal(false);
  };

  const handleLogout = () => {
    sessionStorage.removeItem('isAuthenticated');
    sessionStorage.removeItem('userDisplayName');
    sessionStorage.removeItem('userEmail');
    sessionStorage.removeItem('userAvatar');
    setIsAuthenticated(false);
    setCurrentUser(null);
    setShowAuthModal(true);
  };

  const handleDocumentSelect = (documentId, documentName) => {
    setCurrentDocumentId(documentId);
    setCurrentDocumentName(documentName);
    // Clear users when switching documents or deselecting
    if (!documentId) {
      setUsers([]);
    }
  };

  return (
    <div>
      <Navbar currentUser={currentUser} onLogout={handleLogout} />

      <div style={{ display: "flex", height: "90vh" }}>
        {/* Mobile Toggle Button - Only show on mobile when document is selected */}
        {currentDocumentId && isMobile && (
          <button 
            className="mobile-users-toggle"
            onClick={() => setShowMobileUsers(!showMobileUsers)}
            style={{
              position: 'fixed',
              bottom: '20px',
              right: '20px',
              width: '56px',
              height: '56px',
              borderRadius: '50%',
              backgroundColor: '#007bff',
              color: 'white',
              border: 'none',
              fontSize: '24px',
              cursor: 'pointer',
              boxShadow: '0 4px 12px rgba(0,123,255,0.3)',
              zIndex: 1000,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              transition: 'transform 0.2s ease'
            }}
          >
            👥
          </button>
        )}

        {/* Document List - Hide on mobile when document is selected */}
        {(!isMobile || !currentDocumentId) && (
          <div className="document-list-sidebar" style={{ 
            width: isMobile ? "100%" : "300px", 
            minWidth: isMobile ? "auto" : "300px", 
            maxWidth: isMobile ? "none" : "350px",
            borderRight: isMobile ? "none" : "1px solid #ccc",
            borderBottom: isMobile && currentDocumentId ? "1px solid #ccc" : "none",
            overflow: "hidden",
            height: isMobile ? "auto" : "100%"
          }}>
            <DocumentList 
              onDocumentSelect={handleDocumentSelect}
              currentDocumentId={currentDocumentId}
              userId={currentUser?.displayName || 'anonymous'}
            />
          </div>
        )}

        {/* Users Sidebar - Desktop Only - Only show when document is selected */}
        {!isMobile && isAuthenticated && currentDocumentId && (
          <div className="desktop-users-sidebar" style={{
            width: '250px',
            minWidth: '250px',
            backgroundColor: '#f8f9fa',
            borderLeft: '1px solid #e0e0e0',
            padding: '20px',
            overflowY: 'auto'
          }}>
            <ActiveUsers 
              users={users} 
              onDocumentSelect={handleDocumentSelect}
              userId={currentUser?.displayName || 'anonymous'}
            />
          </div>
        )}

        {/* Mobile Sidebar Overlay - Only show when document is selected */}
        {showMobileUsers && currentDocumentId && (
          <div className="mobile-sidebar-overlay" onClick={() => setShowMobileUsers(false)}>
            <div className="mobile-sidebar" onClick={(e) => e.stopPropagation()}>
              <div style={{ 
                display: 'flex', 
                justifyContent: 'space-between', 
                alignItems: 'center',
                padding: '15px',
                borderBottom: '1px solid #e0e0e0'
              }}>
                <h3 style={{ margin: 0, color: '#333' }}>Active Users</h3>
                <button 
                  onClick={() => setShowMobileUsers(false)}
                  style={{
                    background: 'none',
                    border: 'none',
                    fontSize: '18px',
                    cursor: 'pointer',
                    color: '#666'
                  }}
                >
                  ×
                </button>
              </div>
              <ActiveUsers 
                users={users} 
                onDocumentSelect={handleDocumentSelect}
                userId={currentUser?.displayName || 'anonymous'}
              />
            </div>
          </div>
        )}

        {/* Editor */}
        <div className="editor-container" style={{ 
          flex: isMobile ? 1 : 1,
          minWidth: "0",
          overflow: "hidden", // Changed from auto to hidden to prevent horizontal scrolling
          width: isMobile ? "100%" : "auto",
          height: isMobile ? "100vh" : "auto",
          maxWidth: isMobile ? "100vw" : "none" // Prevent exceeding viewport width on mobile
        }}>
          {isAuthenticated ? (
            currentDocumentId ? (
              <Editor 
                onUsersUpdate={setUsers} 
                documentId={currentDocumentId}
                documentName={currentDocumentName}
              />
            ) : (
              <div style={{ 
                display: 'flex', 
                alignItems: 'center', 
                justifyContent: 'center',
                height: '100%',
                fontSize: '18px',
                color: '#666',
                flexDirection: 'column',
                gap: '16px'
              }}>
                <div style={{ fontSize: '48px', opacity: 0.3 }}>📄</div>
                <div>Select a document to start editing</div>
                <div style={{ fontSize: '14px', opacity: 0.7 }}>
                  Create a new document or choose an existing one from the list
                </div>
              </div>
            )
          ) : (
            <div style={{ 
              display: 'flex', 
              alignItems: 'center', 
              justifyContent: 'center',
              height: '100%',
              fontSize: '18px',
              color: '#666'
            }}>
              Please authenticate to start collaborating
            </div>
          )}
        </div>
      </div>

      {/* Authentication Modal */}
      {showAuthModal && (
        <AuthModal
          documentId="doc1"
          userId={sessionStorage.getItem('collaborativeUserId') || 'user' + Math.floor(Math.random() * 1000)}
          onAuthComplete={handleAuthComplete}
          onClose={() => setShowAuthModal(false)}
        />
      )}
    </div>
  );
};

export default App;
