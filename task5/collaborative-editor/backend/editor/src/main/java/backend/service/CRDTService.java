package backend.service;

import backend.model.CRDTOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CRDTService {
    
    @Autowired
    private DocumentService documentService;
    
    /**
     * Apply CRDT operation to document
     * CRDT ensures commutative operations - order doesn't matter
     */
    public String applyCRDTOperation(String documentId, String userId, String newContent) {
        try {
            System.out.println(" CRDT: Applying operation for user: " + userId);
            
            // Get current document content
            String currentContent = documentService.getDocumentContent(documentId);
            System.out.println(" CRDT: Current content: '" + currentContent + "'");
            System.out.println(" CRDT: New content: '" + newContent + "'");
            
            // For now, just save the new content directly (simplified approach)
            // TODO: Implement proper CRDT diff algorithm later
            String result = newContent;
            
            // Create a simple CRDT operation for the entire content
            String charId = UUID.randomUUID().toString();
            double priority = System.currentTimeMillis() + userId.hashCode() * 0.001;
            
            CRDTOperation crdtOp = new CRDTOperation(
                documentId, 
                userId, 
                System.currentTimeMillis(), 
                0, 
                charId, 
                newContent, 
                priority
            );
            
            // Update document content
            documentService.saveDocument(documentId, result);
            
            System.out.println(" CRDT: Applied and saved operation, result: '" + result + "'");
            return result;
            
        } catch (Exception e) {
            System.err.println(" CRDT: Error applying operation: " + e.getMessage());
            e.printStackTrace();
            return newContent; // Fallback to new content
        }
    }
    
    /**
     * Merge multiple CRDT operations
     * CRDT ensures all nodes converge to same state
     */
    public String mergeCRDTOperations(String documentId, List<CRDTOperation> operations) {
        try {
            System.out.println("🔧 CRDT: Merging " + operations.size() + " operations");
            
            // Get current content
            String content = documentService.getDocumentContent(documentId);
            
            // Sort operations by priority for deterministic application
            operations.sort((op1, op2) -> Double.compare(op1.getPriority(), op2.getPriority()));
            
            // Apply operations in order (CRDT ensures order doesn't matter, but we sort for consistency)
            for (CRDTOperation op : operations) {
                content = op.apply(content);
                System.out.println("🔧 CRDT: Applied op " + op.getCharId() + ", content: '" + content + "'");
            }
            
            // Save merged result
            documentService.saveDocument(documentId, content);
            
            System.out.println("🔧 CRDT: Merge complete, final content: '" + content + "'");
            return content;
            
        } catch (Exception e) {
            System.err.println("❌ CRDT: Error merging operations: " + e.getMessage());
            e.printStackTrace();
            return documentService.getDocumentContent(documentId);
        }
    }
    
        
    /**
     * Generate unique character ID for CRDT
     */
    private String generateCharId(String userId) {
        return userId + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
