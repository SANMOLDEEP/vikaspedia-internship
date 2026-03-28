import React, { useState, useEffect } from "react";
import UserProfile from "../UserProfile/UserProfile";
import "./ActiveUsers.css";

const ActiveUsers = ({ users, documentId, currentUserId, onDocumentSelect, userId }) => {
  const [showProfile, setShowProfile] = useState(null);
  const [showDocumentsDropdown, setShowDocumentsDropdown] = useState(false);
  const [showUsersDropdown, setShowUsersDropdown] = useState(false);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [newDocumentName, setNewDocumentName] = useState('');
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);

  useEffect(() => {
    const handleResize = () => {
      setIsMobile(window.innerWidth <= 768);
    };
    
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // Fetch documents on component mount (only on mobile)
  useEffect(() => {
    if (isMobile) {
      fetchDocuments();
    }
  }, [isMobile]);

  // Debug: Log users data when it changes
  useEffect(() => {
    console.log("👥 ActiveUsers received users data:", users);
    console.log("👥 User entries:", Object.entries(users));
    
    // Log detailed user object info
    Object.entries(users).forEach(([userId, user]) => {
      console.log("👤 User details for", userId, ":", {
        displayName: user.displayName,
        userId: user.userId,
        isGuest: user.isGuest,
        userColor: user.userColor,
        avatarUrl: user.avatarUrl,
        email: user.email
      });
    });
  }, [users]);

  const fetchDocuments = async () => {
    try {
      setLoading(true);
      const response = await fetch('/api/documents/list');
      if (response.ok) {
        const data = await response.json();
        setDocuments(data);
      } else {
        setError('Failed to fetch documents');
      }
    } catch (err) {
      setError('Error fetching documents: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const createDocument = async (e) => {
    e.preventDefault();
    
    if (!newDocumentName.trim()) {
      setError('Document name is required');
      return;
    }

    try {
      const requestBody = {
        name: newDocumentName.trim(),
        createdBy: userId || 'anonymous'
      };
      
      const response = await fetch('/api/documents/create', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestBody)
      });

      if (response.ok) {
        const newDoc = await response.json();
        setDocuments([newDoc, ...documents]);
        setNewDocumentName('');
        setShowCreateForm(false);
        setError('');
        
        // Auto-select the new document
        if (onDocumentSelect) {
          onDocumentSelect(newDoc.id, newDoc.name);
        }
      } else {
        const errorText = await response.text();
        setError(errorText);
      }
    } catch (err) {
      setError('Error creating document: ' + err.message);
    }
  };

  const handleProfileUpdate = (updatedProfile) => {
    setShowProfile(null);
    // Profile update is handled by the server
  };

  const getUserInitials = (user) => {
    const name = user.displayName || user.userId || 'User';
    return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  };

  const getUserAvatar = (user) => {
    return <div className="user-avatar-text">{getUserInitials(user)}</div>;
  };

  return (
    <div className="active-users">
      {/* Mobile Dropdown Headers - Only show on mobile */}
      {isMobile && (
        <div className="mobile-dropdowns">
          {/* Documents Dropdown */}
          <div className="mobile-dropdown">
            <button 
              className="dropdown-button"
              onClick={() => setShowDocumentsDropdown(!showDocumentsDropdown)}
            >
              📄 Documents {showDocumentsDropdown ? '▲' : '▼'}
            </button>
            {showDocumentsDropdown && (
              <div className="dropdown-content">
                {loading && (
                  <div className="dropdown-item">
                    📄 Loading documents...
                  </div>
                )}
                {!loading && error && (
                  <div className="dropdown-item" style={{ color: 'red' }}>
                    ❌ {error}
                  </div>
                )}
                {!loading && !error && (
                  <>
                    {/* Create New Document Option */}
                    <div 
                      className="dropdown-item create-document-item"
                      onClick={() => {
                        setShowCreateForm(true);
                        setShowDocumentsDropdown(false);
                      }}
                      style={{ 
                        backgroundColor: '#007bff', 
                        color: 'white',
                        fontWeight: 'bold'
                      }}
                    >
                      ➕ Create New Document
                    </div>
                    
                    {/* Existing Documents */}
                    {documents.length === 0 ? (
                      <div className="dropdown-item">
                        📄 No documents found
                      </div>
                    ) : (
                      documents.map(doc => (
                        <div 
                          key={doc.id} 
                          className={`dropdown-item ${doc.id === documentId ? 'current-document' : ''}`}
                          onClick={() => {
                            if (onDocumentSelect) {
                              onDocumentSelect(doc.id, doc.name);
                            }
                            setShowDocumentsDropdown(false);
                          }}
                          style={{ 
                            backgroundColor: doc.id === documentId ? '#e3f2fd' : 'transparent',
                            color: doc.id === documentId ? '#1976d2' : '#333'
                          }}
                        >
                          📄 {doc.name}
                          {doc.id === documentId && (
                            <span style={{ marginLeft: '8px', fontSize: '12px' }}>
                              ✓
                            </span>
                          )}
                        </div>
                      ))
                    )}
                  </>
                )}
              </div>
            )}
          </div>

          {/* Active Users Dropdown */}
          <div className="mobile-dropdown">
            <button 
              className="dropdown-button"
              onClick={() => setShowUsersDropdown(!showUsersDropdown)}
            >
              👥 Active Users ({Object.keys(users).length}) {showUsersDropdown ? '▲' : '▼'}
            </button>
            {showUsersDropdown && (
              <div className="dropdown-content">
                {Object.entries(users).map(([userId, user]) => (
                  <div key={userId} className="dropdown-user-item">
                    <div className="dropdown-user-avatar" style={{ backgroundColor: user.userColor }}>
                      {getUserInitials(user)}
                    </div>
                    <div className="dropdown-user-info">
                      <div className="dropdown-user-name">
                        {user.displayName || user.userId}
                      </div>
                      {user.email && (
                        <div className="dropdown-user-email">
                          {user.email}
                        </div>
                      )}
                      <div className="dropdown-user-status">
                        <span className="dropdown-status-indicator" style={{ backgroundColor: user.userColor }}></span>
                        Active
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Desktop Header - Only show on desktop */}
      {!isMobile && (
        <h3>Active Users ({Object.keys(users).length})</h3>
      )}
      
      {/* Desktop User List - Only show on desktop */}
      {!isMobile && (
        <div className="user-list">
          {Object.entries(users).map(([userId, user]) => (
            <div key={userId} className="user-item">
              <div className="user-avatar-container" style={{ borderColor: user.userColor }}>
                {getUserAvatar(user)}
              </div>
              <div className="user-info">
                <div className="user-name" title={user.displayName || user.userId}>
                  {user.displayName || user.userId}
                </div>
                {user.email && (
                  <div className="user-email" title={user.email}>
                    {user.email}
                  </div>
                )}
                <div className="user-status">
                  <span className="status-indicator" style={{ backgroundColor: user.userColor }}></span>
                  <span className="status-text">Active</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
      
      {showCreateForm && (
        <div className="create-document-modal">
          <div className="create-document-content">
            <h3>Create New Document</h3>
            <form onSubmit={createDocument}>
              <input
                type="text"
                placeholder="Enter document name..."
                value={newDocumentName}
                onChange={(e) => setNewDocumentName(e.target.value)}
                className="create-document-input"
                autoFocus
              />
              {error && (
                <div className="create-document-error">
                  {error}
                </div>
              )}
              <div className="create-document-buttons">
                <button
                  type="button"
                  onClick={() => {
                    setShowCreateForm(false);
                    setNewDocumentName('');
                    setError('');
                  }}
                  className="create-document-cancel"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="create-document-submit"
                >
                  Create
                </button>
              </div>
            </form>
          </div>
          <button 
            onClick={() => {
              setShowCreateForm(false);
              setNewDocumentName('');
              setError('');
            }}
            className="close-modal"
          >
            ❌
          </button>
        </div>
      )}
      
      {showProfile && (
        <div className="profile-modal">
          <UserProfile 
            userId={showProfile}
            documentId={documentId}
            onProfileUpdate={handleProfileUpdate}
          />
          <button 
            onClick={() => setShowProfile(null)}
            className="close-modal"
          >
            ❌
          </button>
        </div>
      )}
    </div>
  );
};

export default ActiveUsers;