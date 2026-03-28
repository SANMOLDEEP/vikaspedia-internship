import React, { useEffect, useState } from 'react';
import { connectSocket } from '../../services/socketService';

const CursorOverlay = ({ documentId, userId }) => {
  const [cursors, setCursors] = useState({});

  useEffect(() => {
    // Subscribe to cursor updates
    connectSocket(documentId, userId, (data) => {
      // This is handled by the main socket connection
    }, (usersList) => {
      // This is handled by the main socket connection
    }, (cursorData) => {
      // Handle cursor updates
      if (Array.isArray(cursorData)) {
        const newCursors = {};
        cursorData.forEach(cursor => {
          if (cursor.position >= 0) { // Don't show removed cursors
            newCursors[cursor.userId] = cursor;
          }
        });
        setCursors(newCursors);
      }
    });

    return () => {
      // Cleanup
    };
  }, [documentId, userId]);

  const renderCursor = (cursor) => {
    const userColor = cursor.userColor || '#007bff';
    const userName = cursor.displayName || cursor.userId;
    
    return (
      <div
        key={cursor.userId}
        className="remote-cursor"
        style={{
          position: 'absolute',
          left: `${cursor.position}px`,
          top: '0px',
          width: '2px',
          height: '20px',
          backgroundColor: userColor,
          pointerEvents: 'none',
          zIndex: 1000,
        }}
      >
        <div
          className="cursor-label"
          style={{
            position: 'absolute',
            top: '-25px',
            left: '2px',
            backgroundColor: userColor,
            color: 'white',
            padding: '2px 6px',
            borderRadius: '3px',
            fontSize: '12px',
            whiteSpace: 'nowrap',
            fontWeight: 'bold',
            boxShadow: '0 2px 4px rgba(0,0,0,0.2)',
          }}
        >
          {userName}
        </div>
      </div>
    );
  };

  const renderSelection = (cursor) => {
    if (!cursor.selectionLength || cursor.selectionLength === 0) {
      return null;
    }

    const userColor = cursor.userColor || '#007bff';
    
    return (
      <div
        key={`selection-${cursor.userId}`}
        className="remote-selection"
        style={{
          position: 'absolute',
          left: `${cursor.position}px`,
          top: '0px',
          width: `${cursor.selectionLength}px`,
          height: '20px',
          backgroundColor: `${userColor}33`, // Add transparency
          pointerEvents: 'none',
          zIndex: 999,
        }}
      />
    );
  };

  return (
    <div className="cursor-overlay">
      {Object.values(cursors)
        .filter(cursor => cursor.userId !== userId) // Don't show own cursor
        .map(cursor => (
          <React.Fragment key={cursor.userId}>
            {renderSelection(cursor)}
            {renderCursor(cursor)}
          </React.Fragment>
        ))}
    </div>
  );
};

export default CursorOverlay;
