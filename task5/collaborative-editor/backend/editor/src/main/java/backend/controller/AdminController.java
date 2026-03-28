package backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import backend.service.DocumentService;

/**
 * Admin controller for manual database cleanup
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    @Autowired
    private DocumentService documentService;
    
    /**
     * Clean corrupted document content
     */
    @PostMapping("/cleanup/{documentId}")
    public ResponseEntity<String> cleanupDocument(@PathVariable String documentId) {
        try {
            // Get current content
            String currentContent = documentService.getDocumentContent(documentId);
            
            // Check if content is corrupted (contains JSON operations)
            if (currentContent != null && 
                (currentContent.startsWith("{\"type\":") || 
                 currentContent.startsWith("[{\"type\":") ||
                 currentContent.contains("\"operation_type\":") ||
                 currentContent.contains("\"sequenceNumber\":"))) {
                
                // Clean the content
                documentService.saveDocument(documentId, "");
                
                return ResponseEntity.ok("Document cleaned successfully. Content was: " + 
                    currentContent.substring(0, Math.min(100, currentContent.length())) + "...");
            } else {
                return ResponseEntity.ok("Document content is already clean: " + 
                    (currentContent != null ? currentContent.substring(0, Math.min(50, currentContent.length())) : "null"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error cleaning document: " + e.getMessage());
        }
    }
    
    /**
     * Get document content for debugging
     */
    @GetMapping("/debug/{documentId}")
    public ResponseEntity<String> getDocumentDebug(@PathVariable String documentId) {
        try {
            String content = documentService.getDocumentContent(documentId);
            return ResponseEntity.ok("Document content: " + content + " (length: " + 
                (content != null ? content.length() : 0) + ")");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error getting document: " + e.getMessage());
        }
    }
}
