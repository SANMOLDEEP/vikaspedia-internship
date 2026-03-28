import React, { useState } from 'react';
import { sendMessage } from '../../services/socketService';
import './AuthModal.css';

const AuthModal = ({ onAuthComplete, onClose }) => {
  const [displayName, setDisplayName] = useState('');
  const [email, setEmail] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const isValidEmail = (email) => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');

    try {
      // Validate form
      if (!displayName || displayName.trim().length < 2) {
        setError('Name must be at least 2 characters');
        setIsLoading(false);
        return;
      }

      // Validate email (required)
      if (!email || !email.trim()) {
        setError('Email is required');
        setIsLoading(false);
        return;
      }
      
      if (!isValidEmail(email.trim())) {
        setError('Please enter a valid email address');
        setIsLoading(false);
        return;
      }

      // Prepare user data (name and email)
      const authData = {
        displayName: displayName.trim(),
        email: email.trim() || ''
      };

      // Store user info in session immediately
      sessionStorage.setItem('userDisplayName', displayName.trim());
      sessionStorage.setItem('userEmail', email.trim() || '');
      sessionStorage.setItem('isAuthenticated', 'true');

      // Call onAuthComplete to update parent state
      onAuthComplete(authData);
      
      // Close modal immediately
      onClose();

    } catch (err) {
      setError('Something went wrong. Please try again.');
      setIsLoading(false);
    }
  };

  return (
    <div className="auth-modal-overlay">
      <div className="auth-modal">
        <h2>Join Collaborative Editor</h2>
        <form onSubmit={handleSubmit}>
          {error && <div className="error-message">{error}</div>}
          
          <div className="form-group">
            <label htmlFor="displayName">Your Name *</label>
            <input
              type="text"
              id="displayName"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              placeholder="Enter your name"
              required
              autoFocus
            />
          </div>

          <div className="form-group">
            <label htmlFor="email">Email *</label>
            <input
              type="email"
              id="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="Enter your email"
              required
            />
          </div>

          <button type="submit" disabled={!displayName.trim() || !email.trim() || isLoading}>
            {isLoading ? 'Joining...' : 'Start Editing'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default AuthModal;
