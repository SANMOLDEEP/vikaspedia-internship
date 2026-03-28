import React, { useState } from "react";

const Navbar = ({ currentUser, onLogout }) => {
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);

  React.useEffect(() => {
    const handleResize = () => {
      setIsMobile(window.innerWidth <= 768);
    };
    
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const mobileStyles = isMobile ? {
    padding: "12px 16px",
    fontSize: "18px"
  } : {
    padding: "10px 20px",
    fontSize: "24px"
  };

  return (
    <nav
      style={{
        backgroundColor: "#007bff",
        color: "white",
        padding: mobileStyles.padding,
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
        minHeight: isMobile ? "60px" : "auto",
      }}
    >
      <h1 style={{ 
        margin: 0, 
        fontSize: mobileStyles.fontSize,
        fontWeight: "bold"
      }}>
        Collaborative Editor
      </h1>
      
      <div style={{ 
        display: "flex", 
        gap: isMobile ? "10px" : "15px", 
        alignItems: "center"
      }}>
        {!isMobile && (
          <span style={{ fontSize: "14px" }}>Real-time Collaboration</span>
        )}
        
        {currentUser && (
          <div style={{ 
            display: "flex", 
            alignItems: "center", 
            gap: isMobile ? "8px" : "10px",
            backgroundColor: "rgba(255,255,255,0.1)",
            padding: isMobile ? "8px 12px" : "5px 12px",
            borderRadius: "20px",
            minWidth: isMobile ? "auto" : "200px"
          }}>
            {currentUser.avatarUrl ? (
              <img 
                src={currentUser.avatarUrl} 
                alt="User avatar" 
                style={{ 
                  width: isMobile ? "28px" : "24px", 
                  height: isMobile ? "28px" : "24px", 
                  borderRadius: "50%",
                  objectFit: "cover"
                }}
              />
            ) : (
              <div style={{
                width: isMobile ? "28px" : "24px",
                height: isMobile ? "28px" : "24px",
                borderRadius: "50%",
                backgroundColor: "#fff",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontSize: isMobile ? "14px" : "12px",
                fontWeight: "bold",
                color: "#007bff"
              }}>
                {currentUser.displayName.charAt(0).toUpperCase()}
              </div>
            )}
            
            {!isMobile && (
              <span style={{ fontSize: "14px", fontWeight: "500" }}>
                {currentUser.displayName}
              </span>
            )}
            
            <button
              onClick={onLogout}
              style={{
                background: "rgba(255,255,255,0.2)",
                border: "none",
                color: "white",
                padding: isMobile ? "8px 12px" : "4px 8px",
                borderRadius: "4px",
                cursor: "pointer",
                fontSize: isMobile ? "14px" : "12px",
                transition: "background 0.2s",
                minWidth: isMobile ? "60px" : "auto",
                height: isMobile ? "36px" : "auto",
                display: "flex",
                alignItems: "center",
                justifyContent: "center"
              }}
              onMouseOver={(e) => e.target.style.background = "rgba(255,255,255,0.3)"}
              onMouseOut={(e) => e.target.style.background = "rgba(255,255,255,0.2)"}
              title="Logout"
            >
              {isMobile ? "🚪" : "🚪 Logout"}
            </button>
          </div>
        )}
      </div>
    </nav>
  );
};

export default Navbar;
