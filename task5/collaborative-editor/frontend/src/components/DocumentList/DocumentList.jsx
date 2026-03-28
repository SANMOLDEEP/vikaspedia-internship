import React, { useState, useEffect } from 'react';
import './DocumentList.css';

const DocumentList = ({ onDocumentSelect, currentDocumentId, userId }) => {
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [newDocumentName, setNewDocumentName] = useState('');
  const [error, setError] = useState('');

  // Fetch documents on component mount
  useEffect(() => {
    fetchDocuments();
  }, []);

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
    
    console.log("🚀 Create document clicked");
    console.log("🚀 Document name:", newDocumentName);
    console.log("🚀 User ID:", userId);
    
    if (!newDocumentName.trim()) {
      setError('Document name is required');
      return;
    }

    try {
      const requestBody = {
        name: newDocumentName.trim(),
        createdBy: userId || 'anonymous'
      };
      
      console.log("🚀 Sending request:", requestBody);
      console.log("🚀 Request URL:", '/api/documents/create');
      console.log("🚀 Full URL:", window.location.origin + '/api/documents/create');
      
      const response = await fetch('/api/documents/create', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestBody)
      });
      
      console.log("🚀 Response status:", response.status);
      console.log("🚀 Response ok:", response.ok);

      if (response.ok) {
        const newDoc = await response.json();
        console.log("🚀 Document created successfully:", newDoc);
        setDocuments([newDoc, ...documents]);
        setNewDocumentName('');
        setShowCreateForm(false);
        setError('');
        
        // Auto-select the new document
        onDocumentSelect(newDoc.id, newDoc.name);
      } else {
        const errorText = await response.text();
        console.error("🚀 Create document error:", errorText);
        setError(errorText);
      }
    } catch (err) {
      console.error("🚀 Create document exception:", err);
      setError('Error creating document: ' + err.message);
    }
  };

  const deleteDocument = async (documentId) => {
    if (!confirm('Are you sure you want to delete this document?')) {
      return;
    }

    try {
      const response = await fetch(`/api/documents/${documentId}`, {
        method: 'DELETE'
      });

      if (response.ok) {
        setDocuments(documents.filter(doc => doc.id !== documentId));
        if (currentDocumentId === documentId) {
          // Select the first available document or clear selection
          const remainingDocs = documents.filter(doc => doc.id !== documentId);
          if (remainingDocs.length > 0) {
            onDocumentSelect(remainingDocs[0].id, remainingDocs[0].name);
          } else {
            onDocumentSelect(null, null);
          }
        }
      } else {
        setError('Failed to delete document');
      }
    } catch (err) {
      setError('Error deleting document: ' + err.message);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'Unknown';
    const date = new Date(dateString);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  if (loading) {
    return (
      <div className="document-list">
        <div className="loading">Loading documents...</div>
      </div>
    );
  }

  return (
    <div className="document-list">
      <div className="document-list-header">
        <h3>Documents</h3>
        <button 
          className="create-btn"
          onClick={() => setShowCreateForm(!showCreateForm)}
        >
          + New Document
        </button>
      </div>

      {error && (
        <div className="error-message">
          {error}
          <button onClick={() => setError('')}>×</button>
        </div>
      )}

      {showCreateForm && (
        <form className="create-form" onSubmit={createDocument}>
          <input
            type="text"
            placeholder="Document name..."
            value={newDocumentName}
            onChange={(e) => setNewDocumentName(e.target.value)}
            autoFocus
          />
          <button type="submit">Create</button>
          <button type="button" onClick={() => {
            setShowCreateForm(false);
            setNewDocumentName('');
            setError('');
          }}>Cancel</button>
        </form>
      )}

      <div className="documents">
        {documents.length === 0 ? (
          <div className="no-documents">
            <p>No documents yet. Create your first document!</p>
          </div>
        ) : (
          documents.map(doc => (
            <div 
              key={doc.id}
              className={`document-item ${currentDocumentId === doc.id ? 'active' : ''}`}
              onClick={() => onDocumentSelect(doc.id, doc.name)}
            >
              <div className="document-info">
                <h4>{doc.name}</h4>
                <div className="document-meta">
                  <span>Created by: {doc.createdBy}</span>
                  <span>Updated: {formatDate(doc.updatedAt)}</span>
                </div>
              </div>
              <button 
                className="delete-btn"
                onClick={(e) => {
                  e.stopPropagation();
                  deleteDocument(doc.id);
                }}
                title="Delete document"
              >
                🗑️
              </button>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default DocumentList;
