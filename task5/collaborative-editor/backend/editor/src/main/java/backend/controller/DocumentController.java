package backend.controller;

import backend.model.DocumentMetadata;
import backend.service.MultiDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {
    
    @Autowired
    private MultiDocumentService multiDocumentService;
    
    /**
     * Create a new document
     */
    @PostMapping("/create")
    public ResponseEntity<?> createDocument(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String createdBy = request.get("createdBy");
            
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Document name is required");
            }
            
            if (createdBy == null || createdBy.trim().isEmpty()) {
                createdBy = "anonymous";
            }
            
            DocumentMetadata document = multiDocumentService.createDocument(name.trim(), createdBy);
            return ResponseEntity.ok(document);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error creating document: " + e.getMessage());
        }
    }
    
    /**
     * Get all documents
     */
    @GetMapping("/list")
    public ResponseEntity<List<DocumentMetadata>> getAllDocuments() {
        List<DocumentMetadata> documents = multiDocumentService.getAllDocuments();
        return ResponseEntity.ok(documents);
    }
    
    /**
     * Get documents created by a specific user
     */
    @GetMapping("/user/{createdBy}")
    public ResponseEntity<List<DocumentMetadata>> getDocumentsByUser(@PathVariable String createdBy) {
        List<DocumentMetadata> documents = multiDocumentService.getDocumentsByUser(createdBy);
        return ResponseEntity.ok(documents);
    }
    
    /**
     * Get document by ID
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<?> getDocumentById(@PathVariable String documentId) {
        Optional<DocumentMetadata> document = multiDocumentService.getDocumentById(documentId);
        if (document.isPresent()) {
            return ResponseEntity.ok(document.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get document content
     */
    @GetMapping("/{documentId}/content")
    public ResponseEntity<String> getDocumentContent(@PathVariable String documentId) {
        String content = multiDocumentService.getDocumentContent(documentId);
        return ResponseEntity.ok(content);
    }
    
    /**
     * Save document content
     */
    @PostMapping("/{documentId}/content")
    public ResponseEntity<?> saveDocumentContent(
            @PathVariable String documentId,
            @RequestBody Map<String, String> request) {
        try {
            String content = request.get("content");
            if (content == null) {
                content = "";
            }
            
            multiDocumentService.saveDocumentContent(documentId, content, "system");
            return ResponseEntity.ok("Content saved successfully");
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error saving content: " + e.getMessage());
        }
    }
    
    /**
     * Delete document
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<?> deleteDocument(@PathVariable String documentId) {
        try {
            multiDocumentService.deleteDocument(documentId);
            return ResponseEntity.ok("Document deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error deleting document: " + e.getMessage());
        }
    }
    
    /**
     * Search documents
     */
    @GetMapping("/search/{searchTerm}")
    public ResponseEntity<List<DocumentMetadata>> searchDocuments(@PathVariable String searchTerm) {
        List<DocumentMetadata> documents = multiDocumentService.searchDocuments(searchTerm);
        return ResponseEntity.ok(documents);
    }
}
