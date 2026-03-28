package backend.service;

import backend.model.DocumentMetadata;
import backend.model.ContentHistory;
import backend.model.User;
import backend.repository.DocumentMetadataRepository;
import backend.repository.ContentHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class MultiDocumentService {
    
    @Autowired
    private DocumentMetadataRepository documentMetadataRepository;
    
    @Autowired
    private ContentHistoryRepository contentHistoryRepository;
    
    @Autowired
    private UserService userService;
    
    /**
     * Create a new document
     */
    public DocumentMetadata createDocument(String name, String createdBy) {
        // Check if document name already exists
        if (documentMetadataRepository.existsByName(name)) {
            throw new IllegalArgumentException("Document with name '" + name + "' already exists");
        }
        
        // Generate unique ID
        String documentId = "doc_" + UUID.randomUUID().toString().substring(0, 8);
        
        // Create document metadata
        DocumentMetadata document = new DocumentMetadata(documentId, name, createdBy);
        document.setContent(""); // Start with empty content
        
        // Save metadata
        DocumentMetadata savedDocument = documentMetadataRepository.save(document);
        
                
        System.out.println("📄 Created new document: " + name + " (ID: " + documentId + ")");
        return savedDocument;
    }
    
    /**
     * Get all documents
     */
    public List<DocumentMetadata> getAllDocuments() {
        return documentMetadataRepository.findByOrderByUpdatedAtDesc();
    }
    
    /**
     * Get documents created by a specific user
     */
    public List<DocumentMetadata> getDocumentsByUser(String createdBy) {
        return documentMetadataRepository.findByCreatedByOrderByUpdatedAtDesc(createdBy);
    }
    
    /**
     * Get document by ID
     */
    public Optional<DocumentMetadata> getDocumentById(String documentId) {
        return documentMetadataRepository.findById(documentId);
    }
    
    /**
     * Get document content
     */
    public String getDocumentContent(String documentId) {
        // Get content from metadata table
        Optional<DocumentMetadata> docOpt = documentMetadataRepository.findById(documentId);
        return docOpt.map(DocumentMetadata::getContent).orElse("");
    }
    
    /**
     * Save document content - creates new row for each update (keeps only last 20 entries)
     */
    @Transactional
    public void saveDocumentContent(String documentId, String content, String userId) {
        System.out.println("🔧 saveDocumentContent called with ID: " + documentId + ", content: '" + content + "', userId: " + userId);
        
        try {
            // Get username for display
            String username = getUsername(userId);
            
            // Create a new content history record
            ContentHistory contentHistory = new ContentHistory(documentId, userId, username, content, "UPDATE");
            System.out.println("🔧 Creating ContentHistory object: " + contentHistory);
            
            ContentHistory saved = contentHistoryRepository.save(contentHistory);
            System.out.println("💾 Created new content history record with ID: " + saved.getId());
            System.out.println("💾 Content history details: " + saved);
            
            // Check if we exceed 50 entries and cleanup if needed
            checkAndCleanupContentHistory(documentId);
            
            // Also update the main document for quick access
            Optional<DocumentMetadata> docOpt = documentMetadataRepository.findById(documentId);
            if (docOpt.isPresent()) {
                DocumentMetadata document = docOpt.get();
                String existingContent = document.getContent();
                
                System.out.println("🔧 Existing content in document: '" + existingContent + "'");
                System.out.println("🔧 New content to update: '" + content + "'");
                
                // Update document metadata
                document.setContent(content);
                documentMetadataRepository.save(document);
                System.out.println("💾 Updated document metadata: '" + content + "'");
            } else {
                System.out.println("⚠️ Document not found with ID: " + documentId);
            }
            
            System.out.println("💾 Total content history records for document: " + contentHistoryRepository.countByDocumentId(documentId));
                        
        } catch (Exception e) {
            System.err.println("❌ Error saving content: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Delete document
     */
    public void deleteDocument(String documentId) {
        // Delete content history first
        contentHistoryRepository.deleteByDocumentId(documentId);
        // Then delete document
        documentMetadataRepository.deleteById(documentId);
        System.out.println("🗑️ Deleted document and content history: " + documentId);
    }
    
    /**
     * Check if content history exceeds 50 entries for a document and cleanup if needed
     */
    @Transactional
    private void checkAndCleanupContentHistory(String documentId) {
        try {
            // Get total count of content history records for this document
            long totalCount = contentHistoryRepository.countByDocumentId(documentId);
            
            if (totalCount > 50) {
                System.out.println("🔍 Content history count (" + totalCount + ") for document " + documentId + " exceeds 50 - running cleanup");
                
                // Get the IDs of the last 50 records for this document
                List<Long> last50Ids = contentHistoryRepository.findLast50ContentHistoryIdsByDocument(documentId);
                
                // Get all records for this document to find which ones to delete
                List<ContentHistory> allRecords = contentHistoryRepository.findByDocumentIdOrderByCreatedAtDesc(documentId);
                
                // Find records to delete (those not in the last 50)
                List<Long> idsToDelete = new ArrayList<>();
                for (ContentHistory record : allRecords) {
                    if (!last50Ids.contains(record.getId())) {
                        idsToDelete.add(record.getId());
                    }
                }
                
                // Delete the excess records
                if (!idsToDelete.isEmpty()) {
                    contentHistoryRepository.deleteByIdIn(idsToDelete);
                    System.out.println("🧹 Cleaned up " + idsToDelete.size() + " excess content history entries for document: " + documentId + " - keeping only last 50 entries");
                } else {
                    System.out.println("🧹 Content history for document " + documentId + " already has 50 or fewer records - no cleanup needed");
                }
            } else {
                System.out.println("🔍 Content history count (" + totalCount + ") for document " + documentId + " is within limit - no cleanup needed");
            }
        } catch (Exception e) {
            System.err.println("❌ Error checking/cleaning up content history: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Clean up old content history (keep only last 50 entries per document) - legacy method for manual cleanup
     */
    @Transactional
    public void cleanupOldContentHistory(String documentId) {
        try {
            // Get all records for this document
            List<ContentHistory> allRecords = contentHistoryRepository.findByDocumentIdOrderByCreatedAtDesc(documentId);
            
            if (allRecords.size() > 50) {
                // Get the IDs of the last 50 records
                List<Long> last50Ids = contentHistoryRepository.findLast50ContentHistoryIdsByDocument(documentId);
                
                // Find records to delete (those not in the last 50)
                List<Long> idsToDelete = new ArrayList<>();
                for (ContentHistory record : allRecords) {
                    if (!last50Ids.contains(record.getId())) {
                        idsToDelete.add(record.getId());
                    }
                }
                
                // Delete the excess records
                if (!idsToDelete.isEmpty()) {
                    // Delete records one by one to avoid issues with deleteByIdIn
                    for (Long id : idsToDelete) {
                        contentHistoryRepository.deleteById(id);
                    }
                    System.out.println("🧹 Cleaned up " + idsToDelete.size() + " excess content history entries for document: " + documentId + " - keeping only last 50");
                } else {
                    System.out.println("🧹 Content history for document " + documentId + " already has 50 or fewer records - no cleanup needed");
                }
            } else {
                System.out.println("🧹 Content history for document " + documentId + " has " + allRecords.size() + " records - no cleanup needed");
            }
        } catch (Exception e) {
            System.err.println("❌ Error cleaning up content history: " + e.getMessage());
        }
    }
    
    /**
     * Clean up excess content history globally (keep only last 50 per document)
     */
    @Transactional
    public void cleanupExcessContentHistory() {
        try {
            // Get all distinct document IDs
            List<String> documentIds = contentHistoryRepository.findAllDistinctDocumentIds();
            int totalCleaned = 0;
            
            for (String documentId : documentIds) {
                // Get all records for this document
                List<ContentHistory> allRecords = contentHistoryRepository.findByDocumentIdOrderByCreatedAtDesc(documentId);
                
                if (allRecords.size() > 50) {
                    // Get the IDs of the last 50 records
                    List<Long> last50Ids = contentHistoryRepository.findLast50ContentHistoryIdsByDocument(documentId);
                    
                    // Find records to delete (those not in the last 50)
                    List<Long> idsToDelete = new ArrayList<>();
                    for (ContentHistory record : allRecords) {
                        if (!last50Ids.contains(record.getId())) {
                            idsToDelete.add(record.getId());
                        }
                    }
                    
                    // Delete the excess records
                    if (!idsToDelete.isEmpty()) {
                        // Delete records one by one to avoid issues with deleteByIdIn
                        for (Long id : idsToDelete) {
                            contentHistoryRepository.deleteById(id);
                        }
                        totalCleaned += idsToDelete.size();
                    }
                }
            }
            
            if (totalCleaned > 0) {
                System.out.println("🧹 Cleaned up " + totalCleaned + " excess content history entries globally - keeping only last 50 per document");
            } else {
                System.out.println("🧹 Content history already has 50 or fewer records per document - no cleanup needed");
            }
        } catch (Exception e) {
            System.err.println("❌ Error cleaning up content history globally: " + e.getMessage());
        }
    }
    
        
    /**
     * Get content history for a document
     */
    public List<ContentHistory> getContentHistory(String documentId) {
        return contentHistoryRepository.findByDocumentIdOrderByCreatedAtAsc(documentId);
    }
    
    /**
     * Get latest content from history
     */
    public String getLatestContentFromHistory(String documentId) {
        Optional<ContentHistory> latest = contentHistoryRepository.findLatestContent(documentId);
        return latest.map(ContentHistory::getContent).orElse("");
    }
    
    /**
     * Get username from user ID
     */
    private String getUsername(String userId) {
        try {
            User user = userService.getUserInfo(userId);
            if (user != null && user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
                return user.getDisplayName();
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error getting username for userId: " + userId + ", error: " + e.getMessage());
        }
        // Fallback to userId if username not found
        return userId;
    }
    
    /**
     * Search documents by name
     */
    public List<DocumentMetadata> searchDocuments(String searchTerm) {
        return documentMetadataRepository.findByNameContainingIgnoreCase(searchTerm);
    }
}
