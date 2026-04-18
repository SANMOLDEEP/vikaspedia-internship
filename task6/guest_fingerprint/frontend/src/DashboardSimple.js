import React, { useEffect, useState } from "react";
import axios from "axios";
import { getAdvancedFingerprint } from './utils/advancedFingerprint';
import { getBrowserProfile } from './utils/fingerprint';
import { trackEvent } from './utils/advancedFingerprint';

// API Base URL - configurable for different environments
const API_BASE_URL = window.location.hostname === 'localhost' 
  ? 'http://localhost:8080' 
  : `http://${window.location.hostname}:8080`;

function Dashboard() {
  const [events, setEvents] = useState([]);
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);

  // Add state for actual event counts from database
  const [actualEventCount, setActualEventCount] = useState(0);
  const [actualPageViews, setActualPageViews] = useState(0);
  const [actualButtonClicks, setActualButtonClicks] = useState(0);
  const [actualGuestLogins, setActualGuestLogins] = useState(0);
  const [actualUserLogins, setActualUserLogins] = useState(0);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      
      // Get event count first
      const eventCountRes = await axios.get(`${API_BASE_URL}/api/events/count`);
      
      const sessionsRes = await axios.get(`${API_BASE_URL}/api/sessions`);
      
      const eventsRes = await axios.get(`${API_BASE_URL}/api/events`);

      const eventCountData = eventCountRes.data;
      const newEvents = eventsRes.data.events || [];
      const newSessions = sessionsRes.data.sessions || [];
      
      // Handle both old and new API response formats
      const totalCount = eventCountData.totalCount || eventCountData.count || 0;
      const pageViewCount = eventCountData.pageViewCount || 0;
      const buttonClickCount = eventCountData.buttonClickCount || 0;
      const guestLoginCount = eventCountData.guestLoginCount || 0;
      const userLoginCount = eventCountData.userLoginCount || 0;
      
      setEvents(newEvents);
      setSessions(newSessions);
      setActualEventCount(totalCount);
      setActualPageViews(pageViewCount);
      setActualButtonClicks(buttonClickCount);
      setActualGuestLogins(guestLoginCount);
      setActualUserLogins(userLoginCount);
      
    } catch (error) {
      console.error("Error fetching data:", error);
      console.error("Error details:", error.message);
      if (error.response) {
        console.error("Response error:", error.response.status, error.response.data);
      }
      // Show error to user on mobile
      alert("Failed to fetch analytics data. Please check backend connection.");
    } finally {
      setLoading(false);
    }
  };

  const handleLogin = async () => {
    try {
      const fp = await getAdvancedFingerprint();
      const browserProfile = await getBrowserProfile();

      const response = await fetch(`${API_BASE_URL}/api/users/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          email: browserProfile.dynamicEmail,
          name: browserProfile.userName,
          fingerprintId: fp.visitorId,
          loginSource: 'GUEST_LOGIN'  // Explicitly mark as guest login
        })
      });

      const result = await response.json();
      
      if (result.success) {
        // Smart messaging based on backend response
        if (result.message && result.message.includes("already exists")) {
          alert("Already logged in from this device!");
        } else if (result.message && result.message.includes("New user created")) {
          alert("Login and linked! (New device detected)");
        } else {
          alert(result.message || "Login successful!");
        }
        fetchData();
      } else {
        alert("Login failed: " + (result.message || "Unknown error"));
      }
    } catch (error) {
      console.error("Login error:", error);
      alert("Login failed!");
    }
  };

  const handleTestClick = async () => {
    try {
      // Send event using advanced tracking
      await trackEvent("BUTTON_CLICK", { button: "dashboard-test" });
      
      // Add delay to ensure database is updated, then fetch fresh data
      setTimeout(() => {
        fetchData();
      }, 1000);
      
    } catch (error) {
      console.error("Event error:", error);
    }
  };

  const formatTimestamp = (timestamp) => {
    if (!timestamp) return "N/A";
    try {
      const date = new Date(timestamp);
      return date.toLocaleString('en-US', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      });
    } catch (error) {
      console.error("Timestamp parsing error:", error, "Timestamp:", timestamp);
      return "Invalid Date";
    }
  };

  if (loading) {
    return React.createElement('div', { style: { padding: "20px" } }, "Loading...");
  }

  return React.createElement('div', { style: { padding: "20px", fontFamily: "Arial, sans-serif" } }, [
    React.createElement('h1', { 
      key: 'title',
      style: { 
        color: "#333", 
        borderBottom: window.innerWidth <= 768 ? "2px solid #007bff" : "2px solid #007bff", 
        paddingBottom: window.innerWidth <= 768 ? "20px" : "30px", 
        marginBottom: window.innerWidth <= 768 ? "20px" : "30px", 
        fontSize: window.innerWidth <= 768 ? "30px" : "45px"
      } 
    }, "Fingerprinting Analytics Dashboard"),

    React.createElement('div', {
      key: 'buttons',
      style: { marginBottom: "30px", display: "flex", gap: "10px" }
    }, [
      React.createElement('button', {
        key: 'login-btn',
        onClick: handleLogin,
        style: {
          backgroundColor: "#007bff",
          color: "white",
          border: "none",
          padding: "10px 20px",
          borderRadius: "5px",
          cursor: "pointer",
          fontSize: "14px"
        }
      }, "Guest Login"),
      
      React.createElement('button', {
        key: 'test-btn',
        onClick: handleTestClick,
        style: {
          backgroundColor: "#28a745",
          color: "white",
          border: "none",
          padding: "10px 20px",
          borderRadius: "5px",
          cursor: "pointer",
          fontSize: "14px"
        }
      }, "Test Button Click"),
      
      React.createElement('button', {
        key: 'refresh-btn',
        onClick: fetchData,
        style: {
          backgroundColor: "#ffc107",
          color: "black",
          border: "none",
          padding: "10px 20px",
          borderRadius: "5px",
          cursor: "pointer",
          fontSize: "14px"
        }
      }, "Refresh Data")
    ]),

    React.createElement('div', {
      key: 'stats',
      style: { 
        display: "grid", 
        gridTemplateColumns: window.innerWidth <= 768 ? "repeat(2, 1fr)" : "repeat(4, 1fr)", 
        gap: "20px", 
        marginBottom: "30px" 
      }
    }, [
      // First Row - 4 Columns
      React.createElement('div', {
        key: 'users-card',
        style: { 
          padding: '10px', 
          backgroundColor: '#f8f9fa', 
          borderRadius: '4px',
          border: '1px solid #dee2e6',
          textAlign: 'center'
        }
      }, [
        React.createElement('h3', { key: 'users-title', style: { fontSize: window.innerWidth > 768 ? '14px' : '12px', marginBottom: '5px', color: window.innerWidth > 768 ? 'inherit' : 'inherit', fontWeight: window.innerWidth > 768 ? 'bold' : 'normal' } }, "Total Users"),
        React.createElement('h2', { key: 'users-count', style: { fontSize: window.innerWidth > 768 ? '12px' : '10px', margin: "5px 0", color: window.innerWidth > 768 ? 'inherit' : '#000000' } }, sessions.length),
        React.createElement('p', { key: 'users-desc', style: { fontSize: '12px', margin: '0' } }, "Unique Fingerprints")
      ]),
      
      React.createElement('div', {
        key: 'events-card',
        style: { 
          padding: '10px', 
          backgroundColor: '#f8f9fa', 
          borderRadius: '4px',
          border: '1px solid #dee2e6',
          textAlign: 'center'
        }
      }, [
        React.createElement('h3', { key: 'events-title', style: { fontSize: window.innerWidth > 768 ? '14px' : '12px', marginBottom: '5px', color: window.innerWidth > 768 ? 'inherit' : 'inherit', fontWeight: window.innerWidth > 768 ? 'bold' : 'normal' } }, "Total Events"),
        React.createElement('h2', { key: 'events-count', style: { fontSize: window.innerWidth > 768 ? '12px' : '10px', margin: "5px 0", color: window.innerWidth > 768 ? 'inherit' : '#000000' } }, actualEventCount),
        React.createElement('p', { key: 'events-desc', style: { fontSize: '12px', margin: '0' } }, "All User Actions")
      ]),
      
      React.createElement('div', {
        key: 'views-card',
        style: { 
          padding: '10px', 
          backgroundColor: '#f8f9fa', 
          borderRadius: '4px',
          border: '1px solid #dee2e6',
          textAlign: 'center'
        }
      }, [
        React.createElement('h3', { key: 'views-title', style: { fontSize: window.innerWidth > 768 ? '14px' : '12px', marginBottom: '5px', color: window.innerWidth > 768 ? 'inherit' : 'inherit', fontWeight: window.innerWidth > 768 ? 'bold' : 'normal' } }, "Page Views"),
        React.createElement('h2', { key: 'views-count', style: { fontSize: window.innerWidth > 768 ? '12px' : '10px', margin: "5px 0", color: window.innerWidth > 768 ? 'inherit' : '#000000' } }, actualPageViews),
        React.createElement('p', { key: 'views-desc', style: { fontSize: '12px', margin: '0' } }, "Site Visits")
      ]),
      
      React.createElement('div', {
        key: 'clicks-card',
        style: { 
          padding: '10px', 
          backgroundColor: '#f8f9fa', 
          borderRadius: '4px',
          border: '1px solid #dee2e6',
          textAlign: 'center'
        }
      }, [
        React.createElement('h3', { key: 'clicks-title', style: { fontSize: window.innerWidth > 768 ? '14px' : '12px', marginBottom: '5px', color: window.innerWidth > 768 ? 'inherit' : 'inherit', fontWeight: window.innerWidth > 768 ? 'bold' : 'normal' } }, "Button Clicks"),
        React.createElement('h2', { key: 'clicks-count', style: { fontSize: window.innerWidth > 768 ? '12px' : '10px', margin: "5px 0", color: window.innerWidth > 768 ? 'inherit' : '#000000' } }, actualButtonClicks),
        React.createElement('p', { key: 'clicks-desc', style: { fontSize: '12px', margin: '0' } }, "User Interactions")
      ])
    ]),

    // Second Row - 2 Columns
    React.createElement('div', {
      key: 'login-stats',
      style: { 
        display: "grid", 
        gridTemplateColumns: "repeat(2, 1fr)", 
        gap: "20px", 
        marginBottom: "30px" 
      }
    }, [
      React.createElement('div', {
        key: 'guest-logins-card',
        style: { 
          padding: '10px', 
          backgroundColor: '#f8f9fa', 
          borderRadius: '4px',
          border: '1px solid #dee2e6',
          textAlign: 'center'
        }
      }, [
        React.createElement('h3', { key: 'guest-logins-title', style: { fontSize: window.innerWidth > 768 ? '14px' : '12px', marginBottom: '5px', color: window.innerWidth > 768 ? 'inherit' : 'inherit', fontWeight: window.innerWidth > 768 ? 'bold' : 'normal' } }, "Guest Logins"),
        React.createElement('h2', { key: 'guest-logins-count', style: { fontSize: window.innerWidth > 768 ? '12px' : '10px', margin: "5px 0", color: window.innerWidth > 768 ? 'inherit' : '#000000' } }, actualGuestLogins),
        React.createElement('p', { key: 'guest-logins-desc', style: { fontSize: '12px', margin: '0' } }, "Basic Mode Logins")
      ]),
      
      React.createElement('div', {
        key: 'user-logins-card',
        style: { 
          padding: '10px', 
          backgroundColor: '#f8f9fa', 
          borderRadius: '4px',
          border: '1px solid #dee2e6',
          textAlign: 'center'
        }
      }, [
        React.createElement('h3', { key: 'user-logins-title', style: { fontSize: window.innerWidth > 768 ? '14px' : '12px', marginBottom: '5px', color: window.innerWidth > 768 ? 'inherit' : 'inherit', fontWeight: window.innerWidth > 768 ? 'bold' : 'normal' } }, "User Logins"),
        React.createElement('h2', { key: 'user-logins-count', style: { fontSize: window.innerWidth > 768 ? '12px' : '10px', margin: "5px 0", color: window.innerWidth > 768 ? 'inherit' : '#000000' } }, actualUserLogins),
        React.createElement('p', { key: 'user-logins-desc', style: { fontSize: '12px', margin: '0' } }, "Advanced Mode Logins")
      ])
    ]),

    React.createElement('div', {
      key: 'table-container',
      style: { 
        backgroundColor: "white", 
        padding: "20px", 
        borderRadius: "8px", 
        boxShadow: "0 2px 4px rgba(0,0,0,0.1)",
        overflowX: "auto"  // Enable horizontal scroll on mobile
      }
    }, [
      React.createElement('h3', { key: 'table-title', style: { marginBottom: "20px" } }, "Recent Events"),
      
      React.createElement('table', {
        key: 'events-table',
        style: { 
          width: "100%", 
          borderCollapse: "collapse", 
          marginTop: "10px",
          fontSize: window.innerWidth <= 768 ? "12px" : "14px"
        }
      }, [
        React.createElement('thead', { key: 'thead' },
          React.createElement('tr', { style: { backgroundColor: "#f8f9fa" } }, [
            React.createElement('th', { 
              key: 'id-header',
              style: { 
                border: "1px solid #ddd", 
                padding: window.innerWidth > 768 ? "12px" : "8px", 
                textAlign: "left",
                fontSize: window.innerWidth > 768 ? "12px" : "14px",
                color: window.innerWidth > 768 ? "inherit" : "inherit",
                fontWeight: window.innerWidth > 768 ? "normal" : "bold"
              } 
            }, "ID"),
            React.createElement('th', { 
              key: 'type-header',
              style: { 
                border: "1px solid #ddd", 
                padding: window.innerWidth > 768 ? "12px" : "8px", 
                textAlign: "left",
                fontSize: window.innerWidth > 768 ? "12px" : "14px",
                color: window.innerWidth > 768 ? "inherit" : "inherit",
                fontWeight: window.innerWidth > 768 ? "normal" : "bold"
              } 
            }, "Type"),
            React.createElement('th', { 
              key: 'fingerprint-header',
              style: { 
                border: "1px solid #ddd", 
                padding: window.innerWidth > 768 ? "12px" : "8px", 
                textAlign: "left",
                fontSize: window.innerWidth > 768 ? "12px" : "14px",
                color: window.innerWidth > 768 ? "inherit" : "inherit",
                fontWeight: window.innerWidth > 768 ? "normal" : "bold"
              } 
            }, "Fingerprint"),
            React.createElement('th', { 
              key: 'time-header',
              style: { 
                border: "1px solid #ddd", 
                padding: window.innerWidth > 768 ? "12px" : "8px", 
                textAlign: "left",
                fontSize: window.innerWidth > 768 ? "12px" : "14px",
                color: window.innerWidth > 768 ? "inherit" : "inherit",
                fontWeight: window.innerWidth > 768 ? "normal" : "bold"
              } 
            }, "Time")
          ])
        ),
        
        React.createElement('tbody', { key: 'tbody' },
          events.slice(0, 10).map((e) =>
            React.createElement('tr', { key: e.id }, [
              React.createElement('td', { 
                key: 'id',
                style: { 
                  border: "1px solid #ddd", 
                  padding: window.innerWidth > 768 ? "8px" : "4px",
                  fontSize: window.innerWidth > 768 ? "12px" : "14px",
                  color: window.innerWidth > 768 ? "inherit" : "inherit"
                } 
              }, e.id),
              React.createElement('td', { 
                key: 'type',
                style: { 
                  border: "1px solid #ddd", 
                  padding: window.innerWidth > 768 ? "8px" : "4px",
                  fontSize: window.innerWidth > 768 ? "12px" : "14px",
                  color: window.innerWidth > 768 ? "inherit" : "inherit"
                } 
              }, 
                React.createElement('span', {
                  style: {
                    backgroundColor: e.eventType === "PAGE_VIEW" ? "#007bff" : 
                                     e.eventType === "BUTTON_CLICK" ? "#28a745" : "#ffc107",
                    color: "white",
                    padding: "2px 8px",
                    borderRadius: "4px",
                    fontSize: "12px"
                  }
                }, e.eventType)
              ),
              React.createElement('td', { 
                key: 'fingerprint',
                style: { 
                  border: "1px solid #ddd", 
                  padding: window.innerWidth <= 768 ? "4px" : "8px", 
                  fontFamily: "monospace", 
                  fontSize: window.innerWidth <= 768 ? "10px" : "12px" 
                }
              }, e.fingerprintId ? e.fingerprintId.substring(0, 12) + "..." : "N/A"),
              
              React.createElement('td', { 
                key: 'timestamp',
                style: { 
                  border: "1px solid #ddd", 
                  padding: window.innerWidth > 768 ? "8px" : "4px", 
                  fontSize: window.innerWidth > 768 ? "12px" : "14px",
                  color: window.innerWidth > 768 ? "inherit" : "inherit"
                } 
              }, formatTimestamp(e.timestamp))
            ])
          )
        )
      ]),
      
      events.length > 10 && React.createElement('p', { 
        key: 'showing-more',
        style: { textAlign: "center", marginTop: "10px", color: "#666" } 
      }, `Showing 10 of ${actualEventCount} events`)
    ])
  ]);
}

export default Dashboard;
