import React, { useState } from 'react';
import { sendMessage } from '../../services/socketService';

const UserProfile = ({ userId, documentId, onProfileUpdate }) => {
  const [isEditing, setIsEditing] = useState(false);
  const [displayName, setDisplayName] = useState('');
  const [email, setEmail] = useState('');
  const [avatarUrl, setAvatarUrl] = useState('');

  const handleSave = () => {
    // Send profile update to server
    sendMessage({
      documentId,
      userId,
      operation: 'profile',
      text: JSON.stringify({
        displayName,
        email,
        avatarUrl
      })
    });

    setIsEditing(false);
    onProfileUpdate && onProfileUpdate({ displayName, email, avatarUrl });
  };

  const handleRegister = () => {
    // Send registration to server (convert from guest to registered user)
    sendMessage({
      documentId,
      userId,
      operation: 'register',
      text: JSON.stringify({
        displayName,
        email,
        avatarUrl
      })
    });

    setIsEditing(false);
    onProfileUpdate && onProfileUpdate({ displayName, email, avatarUrl });
  };

  if (!isEditing) {
    return (
      <div className="user-profile">
        <button 
          onClick={() => setIsEditing(true)}
          className="edit-profile-btn"
          title="Edit Profile"
        >
          ✏️ Edit Profile
        </button>
      </div>
    );
  }

  return (
    <div className="user-profile-edit">
      <h4>Edit Profile</h4>
      <div className="profile-form">
        <div className="form-group">
          <label>Display Name:</label>
          <input
            type="text"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            placeholder="Enter your name"
            maxLength={50}
          />
        </div>
        <div className="form-group">
          <label>Email:</label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="your@email.com"
            maxLength={100}
          />
        </div>
        <div className="form-group">
          <label>Avatar URL:</label>
          <input
            type="url"
            value={avatarUrl}
            onChange={(e) => setAvatarUrl(e.target.value)}
            placeholder="https://example.com/avatar.jpg"
            maxLength={500}
          />
        </div>
        <div className="form-actions">
          <button onClick={handleSave} className="save-btn">
            💾 Save Profile
          </button>
          <button onClick={handleRegister} className="register-btn">
            📝 Register Account
          </button>
          <button onClick={() => setIsEditing(false)} className="cancel-btn">
            ❌ Cancel
          </button>
        </div>
      </div>
    </div>
  );
};

export default UserProfile;
