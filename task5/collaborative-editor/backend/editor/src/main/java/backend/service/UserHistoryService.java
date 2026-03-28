package backend.service;

import backend.model.User;
import backend.model.UserHistory;
import backend.repository.UserHistoryRepository;
import backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserHistoryService {
    
    @Autowired
    private UserHistoryRepository userHistoryRepository;
    
    @Autowired
    private UserService userService;
    
    /**
     * Record user activity when they join a document
     */
    public void recordUserJoin(String userId, String documentId) {
        try {
            User user = userService.getUser(userId).orElse(userService.getUserInfo(userId));
            UserHistory history = new UserHistory(userId, user.getDisplayName(), user.getEmail(), documentId, "JOIN");
            // Set user information including email
            history.setUserColor(user.getUserColor());
            history.setIsGuest(user.getIsGuest());
            
            userHistoryRepository.save(history);
            System.out.println("👤 Recorded user join: " + userId + " in document: " + documentId);
            
            // Check if we exceed 50 entries and cleanup if needed
            checkAndCleanupUserHistory();
        } catch (Exception e) {
            System.err.println("❌ Error recording user join: " + e.getMessage());
        }
    }
    
    /**
     * Record user activity when they leave a document
     */
    public void recordUserLeave(String userId, String documentId) {
        try {
            User user = userService.getUser(userId).orElse(userService.getUserInfo(userId));
            UserHistory history = new UserHistory(userId, user.getDisplayName(), user.getEmail(), documentId, "LEAVE");
            // Set user information including email
            history.setUserColor(user.getUserColor());
            history.setIsGuest(user.getIsGuest());
            
            userHistoryRepository.save(history);
            System.out.println("👤 Recorded user leave: " + userId + " from document: " + documentId);
            
            // Check if we exceed 50 entries and cleanup if needed
            checkAndCleanupUserHistory();
        } catch (Exception e) {
            System.err.println("❌ Error recording user leave: " + e.getMessage());
        }
    }
    
    /**
     * Record user activity when they edit content
     */
    public void recordUserEdit(String userId, String documentId) {
        try {
            User user = userService.getUser(userId).orElse(userService.getUserInfo(userId));
            UserHistory history = new UserHistory(userId, user.getDisplayName(), user.getEmail(), documentId, "EDIT");
            // Set user information including email
            history.setUserColor(user.getUserColor());
            history.setIsGuest(user.getIsGuest());
            
            userHistoryRepository.save(history);
            System.out.println("👤 Recorded user edit: " + userId + " in document: " + documentId);
            
            // Check if we exceed 50 entries and cleanup if needed
            checkAndCleanupUserHistory();
        } catch (Exception e) {
            System.err.println("❌ Error recording user edit: " + e.getMessage());
        }
    }
    
    /**
     * Record general user activity (heartbeat)
     */
    public void recordUserActivity(String userId, String documentId) {
        try {
            User user = userService.getUser(userId).orElse(userService.getUserInfo(userId));
            UserHistory history = new UserHistory(userId, user.getDisplayName(), user.getEmail(), documentId, "ACTIVE");
            // Set user information including email
            history.setUserColor(user.getUserColor());
            history.setIsGuest(user.getIsGuest());
            
            userHistoryRepository.save(history);
            System.out.println("👤 Recorded user activity: " + userId + " in document: " + documentId);
            
            // Check if we exceed 50 entries and cleanup if needed
            checkAndCleanupUserHistory();
        } catch (Exception e) {
            System.err.println("❌ Error recording user activity: " + e.getMessage());
        }
    }
    
    /**
     * Get last 30 active users globally
     */
    public List<UserHistory> getLast30ActiveUsers() {
        return userHistoryRepository.findLast30ActiveUsers();
    }
    
    /**
     * Get last 30 active users for a specific document
     */
    public List<UserHistory> getLast30ActiveUsersForDocument(String documentId) {
        return userHistoryRepository.findLast30ActiveUsersForDocument(documentId);
    }
    
    /**
     * Get user's last activity
     */
    public Optional<UserHistory> getUserLastActivity(String userId) {
        return userHistoryRepository.findFirstByUserIdOrderByLastActiveDesc(userId);
    }
    
    /**
     * Get user's last activity in a specific document
     */
    public Optional<UserHistory> getUserLastActivityInDocument(String userId, String documentId) {
        return userHistoryRepository.findFirstByUserIdAndDocumentIdOrderByLastActiveDesc(userId, documentId);
    }
    
    /**
     * Count users active in last 24 hours
     */
    public Long countActiveUsersInLast24Hours() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return userHistoryRepository.countActiveUsersSince(since);
    }
    
    /**
     * Check if user history exceeds 50 entries and cleanup if needed
     * Optimized for efficiency like content history cleanup
     */
    private void checkAndCleanupUserHistory() {
        try {
            // Get total count of user history records
            long totalCount = userHistoryRepository.count();
            
            if (totalCount > 50) {
                System.out.println("🔍 User history count (" + totalCount + ") exceeds 50 - running cleanup");
                
                // Get the IDs of the last 50 records (most recent)
                List<Long> last50Ids = userHistoryRepository.findLast50UserHistoryIds();
                
                // Delete all records that are NOT in the last 50 (more efficient approach)
                // This is similar to how content history works - keep recent, delete older
                long deletedCount = userHistoryRepository.deleteByIdNotIn(last50Ids);
                
                if (deletedCount > 0) {
                    System.out.println("🧹 Cleaned up " + deletedCount + " excess user history records - keeping only last 50 entries");
                } else {
                    System.out.println("🧹 User history already has 50 or fewer records - no cleanup needed");
                }
            } else {
                System.out.println("🔍 User history count (" + totalCount + ") is within limit - no cleanup needed");
            }
        } catch (Exception e) {
            System.err.println("❌ Error checking/cleaning up user history: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Clean up excess user history records to keep only last 50 active users (legacy method for manual cleanup)
     */
    public void cleanupExcessUserHistory() {
        try {
            // Get all records
            List<UserHistory> allRecords = userHistoryRepository.findAll();
            
            if (allRecords.size() > 50) {
                // Get the IDs of the last 50 records
                List<Long> last50Ids = userHistoryRepository.findLast50UserHistoryIds();
                
                // Find records to delete (those not in the last 50)
                List<Long> idsToDelete = new ArrayList<>();
                for (UserHistory record : allRecords) {
                    if (!last50Ids.contains(record.getId())) {
                        idsToDelete.add(record.getId());
                    }
                }
                
                // Delete the excess records
                if (!idsToDelete.isEmpty()) {
                    // Delete records one by one to avoid issues with deleteByIdIn
                    for (Long id : idsToDelete) {
                        userHistoryRepository.deleteById(id);
                    }
                    System.out.println("🧹 Cleaned up " + idsToDelete.size() + " excess user history records - keeping only last 50 active users");
                } else {
                    System.out.println("🧹 User history already has 50 or fewer records - no cleanup needed");
                }
            } else {
                System.out.println("🧹 User history has " + allRecords.size() + " records - no cleanup needed");
            }
        } catch (Exception e) {
            System.err.println("❌ Error cleaning up user history: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get active users in a document since a specific time
     */
    public List<Object[]> getActiveUsersInDocumentSince(String documentId, LocalDateTime since) {
        return userHistoryRepository.findActiveUsersInDocumentSince(documentId, since);
    }
}
