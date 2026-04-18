import React, { useState, useEffect, useRef } from 'react';
import DashboardSimple from './DashboardSimple';
import { getAdvancedFingerprint, startSession, trackEvent, detectBot } from './utils/advancedFingerprint';
import { getBrowserProfile } from './utils/fingerprint';
import { getBrowserInfo } from './utils/browserInfo';
import axios from 'axios';

// API Base URL - configurable for different environments
const API_BASE_URL = window.location.hostname === 'localhost' 
  ? 'http://localhost:8080' 
  : `http://${window.location.hostname}:8080`;

function EnhancedApp() {
  const [fingerprint, setFingerprint] = useState(null);
  const [sessionId, setSessionId] = useState(null);
  const [botScore, setBotScore] = useState(null);
  const [browserInfo, setBrowserInfo] = useState(null);
  const [isAdvancedMode, setIsAdvancedMode] = useState(false);
  const initializedRef = useRef(false);

  useEffect(() => {
    if (!initializedRef.current) {
      initializeAdvancedTracking();
      initializedRef.current = true;
    }
  }, []);

  const initializeAdvancedTracking = async () => {
    try {
      // Get advanced fingerprint
      const fp = await getAdvancedFingerprint();
      setFingerprint(fp);

      // Get browser information
      const info = getBrowserInfo();
      setBrowserInfo(info);

      // Detect bot for database storage only (not displayed in UI)
      const bot = detectBot();

      // Start session with bot detection data (stored in database only)
      const session = await startSession({
        isBot: bot.isBot,
        indicators: bot.indicators ? bot.indicators.join(', ') : ''
      });
      setSessionId(session.sessionId || session.id || 'fallback_session_' + Date.now());
      
      if (session.botScore !== undefined) {
        setBotScore(session.botScore);
      } else {
        setBotScore(bot.botScore);
      }

      // Track initial page view with clean data only
      await trackEvent("PAGE_VIEW", { 
        page: "enhanced_dashboard"
      });

    } catch (error) {
      console.error('Error initializing advanced tracking:', error);
      // Set fallback values to prevent UI crashes
      setFingerprint('fallback_fingerprint_' + Date.now());
    }
  };

  const handleLogin = async () => {
    if (!fingerprint) {
      alert('Fingerprint not yet generated. Please wait...');
      return;
    }

    const email = prompt('Enter your email for account stitching:');
    if (!email) return;

    try {
      const browserProfile = await getBrowserProfile();
      
      const response = await axios.post(`${API_BASE_URL}/api/users/login`, {
        email: email,
        name: browserProfile.userName,
        fingerprintId: fingerprint.visitorId || fingerprint,
        loginSource: 'USER_LOGIN'  // Explicitly mark as user login
      });

      if (response.data && response.data.success) {
        const fingerprintDisplay = typeof fingerprint === 'string' ? 
          fingerprint : 
          fingerprint.visitorId;
        alert(`Account stitched successfully!\nEmail: ${email}\nFingerprint: ${fingerprintDisplay}\nMessage: ${response.data.message}`);
      } else {
        alert(`Account stitching failed: ${response.data?.message || 'Unknown error'}`);
      }
    } catch (error) {
      console.error('Login stitching error:', error);
      alert('Error during session creation');
    }
  };

  const handleCollisionDetection = async () => {
    const fingerprint = prompt('Enter fingerprint to check for database collisions:');
    
    // Remove validation to allow empty inputs to be tested on backend
    try {
      const response = await axios.post(`${API_BASE_URL}/api/stitching/detect-collision`, {
        fingerprintId: fingerprint
      });

      const result = response.data;
      let alertMessage = `Database Collision Detection:\n\n`;
      alertMessage += `Success: ${result.success}\n`;
      alertMessage += `Fingerprint: ${result.fingerprintId || 'N/A'}\n`;
      alertMessage += `Message: ${result.message}\n`;
      
      if (result.occurrenceCount !== undefined) {
        alertMessage += `\nOccurrences in Database: ${result.occurrenceCount}`;
      }
      
      if (result.databaseCollision !== undefined) {
        alertMessage += `\nDatabase Collision: ${result.databaseCollision ? 'YES' : 'NO'}`;
      }
      
      if (result.sessionIds) {
        alertMessage += `\nSession IDs: ${result.sessionIds}`;
      }
      
      if (result.userIds) {
        alertMessage += `\nLinked User IDs: ${result.userIds}`;
      }
      
      if (result.ipAddresses) {
        alertMessage += `\nIP Addresses: ${result.ipAddresses}`;
      }
      
      if (result.firstSeen) {
        alertMessage += `\nFirst Seen: ${new Date(result.firstSeen).toLocaleString()}`;
      }
      
      if (result.lastSeen) {
        alertMessage += `\nLast Seen: ${new Date(result.lastSeen).toLocaleString()}`;
      }
      
      alert(alertMessage);
    } catch (error) {
      console.error('Collision detection error:', error);
      alert('Error during collision detection');
    }
  };

  const handleTestEvent = async (eventType) => {
    try {
      await trackEvent(eventType, { 
        testEvent: true
      });
      
      console.log(`Test event sent: ${eventType}`);
    } catch (error) {
      console.error('Test event error:', error);
    }
  };

  return (
    <div style={{ 
      padding: window.innerWidth <= 768 ? '15px' : '20px', 
      fontFamily: 'Arial, sans-serif',
      minHeight: '100vh'
    }}>
      <div style={{ 
        marginBottom: '20px', 
        padding: window.innerWidth <= 768 ? '10px' : '15px', 
        backgroundColor: '#f8f9fa', 
        borderRadius: '8px' 
      }}>
        <h1 style={{ 
          color: '#333', 
          marginBottom: '10px',
          fontSize: window.innerWidth <= 768 ? '32px' : '56px',
          fontWeight: 'bold',
          textShadow: '4px 4px 8px rgba(0,0,0,0.2)',
          letterSpacing: '3px',
          lineHeight: '1.0',
          transform: 'scale(1.05)'
        }}>
          Guest Fingerprinting System
        </h1>
        
        <div style={{ 
          marginBottom: window.innerWidth <= 768 ? '25px' : '40px', 
          marginTop: window.innerWidth <= 768 ? '20px' : '30px',
          textAlign: 'center'
        }}>
          <button
            onClick={() => setIsAdvancedMode(!isAdvancedMode)}
            style={{
              padding: window.innerWidth <= 768 ? '12px 24px' : '10px 20px',
              backgroundColor: isAdvancedMode ? '#dc3545' : '#28a745',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer',
              fontSize: window.innerWidth <= 768 ? '16px' : '14px',
              width: window.innerWidth <= 768 ? '100%' : 'auto',
              maxWidth: '300px'
            }}
          >
            {isAdvancedMode ? 'Basic Mode' : 'Advanced Mode'}
          </button>
        </div>

        {fingerprint && (
          <div style={{ 
            display: window.innerWidth <= 768 ? 'block' : 'flex', 
            gap: window.innerWidth <= 768 ? '15px' : '20px', 
            marginBottom: '20px' 
          }}>
            {/* Fingerprint Information Section */}
            <div style={{ 
              flex: window.innerWidth <= 768 ? 'none' : 2, 
              padding: window.innerWidth <= 768 ? '12px' : '15px', 
              backgroundColor: '#e9ecef', 
              borderRadius: '8px',
              marginBottom: window.innerWidth <= 768 ? '15px' : '0px'
            }}>
              <h3 style={{ 
                fontSize: window.innerWidth <= 768 ? '16px' : '18px',
                marginBottom: window.innerWidth <= 768 ? '10px' : '0px'
              }}>Session Information</h3>
              
              {/* First Row - 4 Columns */}
              <div style={{ 
                display: 'grid', 
                gridTemplateColumns: window.innerWidth <= 768 ? 'repeat(2, 1fr)' : 'repeat(4, 1fr)', 
                gap: '15px', 
                marginBottom: '15px',
                fontSize: window.innerWidth <= 768 ? '12px' : '14px'
              }}>
                <div style={{ 
                  padding: '10px', 
                  backgroundColor: '#f8f9fa', 
                  borderRadius: '4px',
                  border: '1px solid #dee2e6'
                }}>
                  <strong>Fingerprint ID:</strong><br/>
                  <span style={{ wordBreak: 'break-all', fontSize: window.innerWidth <= 768 ? '10px' : '12px', color: window.innerWidth <= 768 ? '#000000' : 'inherit' }}>
                    {fingerprint && typeof fingerprint === 'string' ? fingerprint : fingerprint.visitorId}
                  </span>
                </div>
                <div style={{ 
                  padding: '10px', 
                  backgroundColor: '#f8f9fa', 
                  borderRadius: '4px',
                  border: '1px solid #dee2e6'
                }}>
                  <strong>Session ID:</strong><br/>
                  <span style={{ fontSize: window.innerWidth <= 768 ? '10px' : '12px', color: window.innerWidth <= 768 ? '#000000' : 'inherit' }}>
                    {sessionId}
                  </span>
                </div>
                <div style={{ 
                  padding: '10px', 
                  backgroundColor: '#f8f9fa', 
                  borderRadius: '4px',
                  border: '1px solid #dee2e6'
                }}>
                  <strong>Current Session:</strong><br/>
                  <span style={{ fontSize: window.innerWidth <= 768 ? '10px' : '12px', color: window.innerWidth <= 768 ? '#000000' : 'inherit' }}>
                    Active
                  </span>
                </div>
                <div style={{ 
                  padding: '10px', 
                  backgroundColor: '#f8f9fa', 
                  borderRadius: '4px',
                  border: '1px solid #dee2e6'
                }}>
                  <strong>Browser:</strong><br/>
                  <span style={{ fontSize: window.innerWidth <= 768 ? '10px' : '12px', color: window.innerWidth <= 768 ? '#000000' : 'inherit' }}>
                    {browserInfo?.browser} {browserInfo?.version}
                  </span>
                </div>
              </div>

              {/* Second Row - 4 Columns */}
              <div style={{ 
                display: 'grid', 
                gridTemplateColumns: window.innerWidth <= 768 ? 'repeat(2, 1fr)' : 'repeat(4, 1fr)', 
                gap: '15px', 
                marginBottom: '15px',
                fontSize: window.innerWidth <= 768 ? '12px' : '14px'
              }}>
                <div style={{ 
                  padding: '10px', 
                  backgroundColor: '#f8f9fa', 
                  borderRadius: '4px',
                  border: '1px solid #dee2e6'
                }}>
                  <strong>Operating System:</strong><br/>
                  <span style={{ fontSize: window.innerWidth <= 768 ? '10px' : '12px', color: window.innerWidth <= 768 ? '#000000' : 'inherit' }}>
                    {browserInfo?.os} {browserInfo?.osVersion}
                  </span>
                </div>
                <div style={{ 
                  padding: '10px', 
                  backgroundColor: '#f8f9fa', 
                  borderRadius: '4px',
                  border: '1px solid #dee2e6'
                }}>
                  <strong>Screen Resolution:</strong><br/>
                  <span style={{ fontSize: window.innerWidth <= 768 ? '10px' : '12px', color: window.innerWidth <= 768 ? '#000000' : 'inherit' }}>
                    {browserInfo?.screenResolution}
                  </span>
                </div>
                <div style={{ 
                  padding: '10px', 
                  backgroundColor: '#f8f9fa', 
                  borderRadius: '4px',
                  border: '1px solid #dee2e6'
                }}>
                  <strong>Language:</strong><br/>
                  <span style={{ fontSize: window.innerWidth <= 768 ? '10px' : '12px', color: window.innerWidth <= 768 ? '#000000' : 'inherit' }}>
                    {browserInfo?.language}
                  </span>
                </div>
                <div style={{ 
                  padding: '10px', 
                  backgroundColor: '#f8f9fa', 
                  borderRadius: '4px',
                  border: '1px solid #dee2e6'
                }}>
                  <strong>Timezone:</strong><br/>
                  <span style={{ fontSize: window.innerWidth <= 768 ? '10px' : '12px', color: window.innerWidth <= 768 ? '#000000' : 'inherit' }}>
                    {browserInfo?.timezone}
                  </span>
                </div>
              </div>

              {/* Third Row - Bot Score Aligned Left */}
              <div style={{ 
                textAlign: 'left', 
                padding: '10px', 
                backgroundColor: '#f8f9fa', 
                borderRadius: '4px',
                border: '1px solid #dee2e6',
                fontSize: window.innerWidth <= 768 ? '12px' : '14px'
              }}>
                <strong>Bot Score:</strong> <span style={{ color: window.innerWidth <= 768 ? '#000000' : 'inherit' }}>{botScore !== null ? botScore.toFixed(2) : 'Loading...'}</span>
              </div>
            </div>
          </div>
        )}

        {isAdvancedMode && (
          <div style={{ marginBottom: '20px', padding: '15px', backgroundColor: '#fff3cd', borderRadius: '8px' }}>
            <h3>Advanced Features</h3>
            
            <div style={{ marginBottom: '15px' }}>
              <button 
                onClick={handleLogin}
                style={{ 
                  padding: '10px 20px', 
                  backgroundColor: '#28a745',
                  color: 'white',
                  border: 'none',
                  borderRadius: '5px',
                  cursor: 'pointer',
                  marginRight: '10px'
                }}
              >
                Account Stitching
              </button>
              
              <button 
                onClick={handleCollisionDetection}
                style={{ 
                  padding: '10px 20px', 
                  backgroundColor: '#dc3545',
                  color: 'white',
                  border: 'none',
                  borderRadius: '5px',
                  cursor: 'pointer',
                  marginRight: '10px'
                }}
              >
                Detect Collision
              </button>
            </div>
          </div>
        )}
      </div>

      <DashboardSimple />
    </div>
  );
}

export default EnhancedApp;
