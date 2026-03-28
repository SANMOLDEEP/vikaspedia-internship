package backend.controller;

import backend.model.ContentHistory;
import backend.model.UserHistory;
import backend.repository.ContentHistoryRepository;
import backend.repository.UserHistoryRepository;
import backend.service.MultiDocumentService;
import backend.service.UserHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {
    
    @Autowired
    private MultiDocumentService multiDocumentService;
    
    @Autowired
    private UserHistoryService userHistoryService;
    
    @Autowired
    private UserHistoryRepository userHistoryRepository;
    
    @Autowired
    private ContentHistoryRepository contentHistoryRepository;
    
    @PostMapping("/save/{documentId}")
    public String testSave(@PathVariable String documentId, @RequestBody String content) {
        System.out.println("🧪 Test save - Document ID: " + documentId);
        System.out.println("🧪 Test save - Content: '" + content + "'");
        
        try {
            multiDocumentService.saveDocumentContent(documentId, content, "test_user");
            String savedContent = multiDocumentService.getDocumentContent(documentId);
            System.out.println("🧪 Test save - Saved content: '" + savedContent + "'");
            return "Saved: " + savedContent;
        } catch (Exception e) {
            System.err.println("🧪 Test save - Error: " + e.getMessage());
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
    
    @GetMapping("/get/{documentId}")
    public String testGet(@PathVariable String documentId) {
        System.out.println("🧪 Test get - Document ID: " + documentId);
        
        try {
            String content = multiDocumentService.getDocumentContent(documentId);
            System.out.println("🧪 Test get - Content: '" + content + "'");
            return content;
        } catch (Exception e) {
            System.err.println("🧪 Test get - Error: " + e.getMessage());
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
    
    @GetMapping("/content-history/{documentId}")
    public ResponseEntity<?> getContentHistory(@PathVariable String documentId) {
        try {
            List<ContentHistory> history = multiDocumentService.getContentHistory(documentId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error getting content history: " + e.getMessage());
        }
    }
    
    @GetMapping("/user-history")
    public ResponseEntity<?> getUserHistory() {
        try {
            List<UserHistory> history = userHistoryService.getLast30ActiveUsers();
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error getting user history: " + e.getMessage());
        }
    }
    
    @GetMapping("/user-history/{documentId}")
    public ResponseEntity<?> getUserHistoryForDocument(@PathVariable String documentId) {
        try {
            List<UserHistory> history = userHistoryService.getLast30ActiveUsersForDocument(documentId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error getting user history for document: " + e.getMessage());
        }
    }
    
    @PostMapping("/cleanup-user-history")
    public ResponseEntity<?> cleanupUserHistory() {
        try {
            userHistoryService.cleanupExcessUserHistory();
            return ResponseEntity.ok("User history cleanup completed - keeping only last 50 active users");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error cleaning up user history: " + e.getMessage());
        }
    }
    
    @PostMapping("/cleanup-content-history")
    public ResponseEntity<?> cleanupContentHistory() {
        try {
            multiDocumentService.cleanupExcessContentHistory();
            return ResponseEntity.ok("Content history cleanup completed - keeping only last 50 per document");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error cleaning up content history: " + e.getMessage());
        }
    }
    
    @PostMapping("/cleanup-all")
    public ResponseEntity<?> cleanupAll() {
        try {
            userHistoryService.cleanupExcessUserHistory();
            multiDocumentService.cleanupExcessContentHistory();
            return ResponseEntity.ok("All cleanup completed - user history (last 50) and content history (last 50 per document)");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error in cleanup: " + e.getMessage());
        }
    }
    
    @PostMapping("/cleanup-all-new")
    @Transactional
    public ResponseEntity<?> cleanupAllNew() {
        try {
            // Force cleanup using new 50-entry limits
            long userHistoryCount = userHistoryRepository.count();
            System.out.println("🔍 Current user history count: " + userHistoryCount);
            
            if (userHistoryCount > 50) {
                List<Long> last50Ids = userHistoryRepository.findLast50UserHistoryIds();
                List<UserHistory> allRecords = userHistoryRepository.findAll();
                
                List<Long> idsToDelete = new ArrayList<>();
                for (UserHistory record : allRecords) {
                    if (!last50Ids.contains(record.getId())) {
                        idsToDelete.add(record.getId());
                    }
                }
                
                if (!idsToDelete.isEmpty()) {
                    userHistoryRepository.deleteByIdIn(idsToDelete);
                    System.out.println("🧹 Cleaned up " + idsToDelete.size() + " excess user history records - keeping only last 50 entries");
                }
            }
            
            // Clean up content history for each document
            List<String> documentIds = contentHistoryRepository.findAllDistinctDocumentIds();
            int totalContentCleaned = 0;
            
            for (String documentId : documentIds) {
                long contentCount = contentHistoryRepository.countByDocumentId(documentId);
                System.out.println("🔍 Content history count for document " + documentId + ": " + contentCount);
                
                if (contentCount > 50) {
                    List<Long> last50Ids = contentHistoryRepository.findLast50ContentHistoryIdsByDocument(documentId);
                    List<ContentHistory> allRecords = contentHistoryRepository.findByDocumentIdOrderByCreatedAtDesc(documentId);
                    
                    List<Long> idsToDelete = new ArrayList<>();
                    for (ContentHistory record : allRecords) {
                        if (!last50Ids.contains(record.getId())) {
                            idsToDelete.add(record.getId());
                        }
                    }
                    
                    if (!idsToDelete.isEmpty()) {
                        contentHistoryRepository.deleteByIdIn(idsToDelete);
                        totalContentCleaned += idsToDelete.size();
                        System.out.println("🧹 Cleaned up " + idsToDelete.size() + " excess content history entries for document: " + documentId + " - keeping only last 50 entries");
                    }
                }
            }
            
            return ResponseEntity.ok("New cleanup completed - user history (last 50) and content history (last 50 per document). Cleaned " + totalContentCleaned + " content entries.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error in new cleanup: " + e.getMessage());
        }
    }
    
    @GetMapping("/count")
    public ResponseEntity<?> getCounts() {
        try {
            long userHistoryCount = userHistoryRepository.count();
            List<String> documentIds = contentHistoryRepository.findAllDistinctDocumentIds();
            
            Map<String, Object> counts = new HashMap<>();
            counts.put("userHistoryCount", userHistoryCount);
            counts.put("contentHistoryDocuments", documentIds.size());
            
            Map<String, Long> contentCounts = new HashMap<>();
            for (String documentId : documentIds) {
                long contentCount = contentHistoryRepository.countByDocumentId(documentId);
                contentCounts.put(documentId, contentCount);
            }
            counts.put("contentHistoryCounts", contentCounts);
            
            return ResponseEntity.ok(counts);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error getting counts: " + e.getMessage());
        }
    }
    
    @GetMapping("/debug-cleanup")
    public ResponseEntity<?> debugCleanup() {
        try {
            long totalCount = userHistoryRepository.count();
            List<Long> last50Ids = userHistoryRepository.findLast50UserHistoryIds();
            List<UserHistory> allRecords = userHistoryRepository.findAll();
            
            Map<String, Object> debug = new HashMap<>();
            debug.put("totalCount", totalCount);
            debug.put("last50Ids", last50Ids);
            debug.put("allRecordsCount", allRecords.size());
            
            List<Long> idsToDelete = new ArrayList<>();
            for (UserHistory record : allRecords) {
                if (!last50Ids.contains(record.getId())) {
                    idsToDelete.add(record.getId());
                }
            }
            
            debug.put("idsToDelete", idsToDelete);
            debug.put("idsToDeleteCount", idsToDelete.size());
            
            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error debugging cleanup: " + e.getMessage());
        }
    }
}
