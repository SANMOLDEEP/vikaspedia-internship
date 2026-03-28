package backend.repository;

import backend.model.UserHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserHistoryRepository extends JpaRepository<UserHistory, Long> {
    
    // Find user history by user ID
    List<UserHistory> findByUserIdOrderByLastActiveDesc(String userId);
    
    // Find user history by document ID
    List<UserHistory> findByDocumentIdOrderByLastActiveDesc(String documentId);
    
    // Find all user history ordered by last active time
    List<UserHistory> findAllByOrderByLastActiveDesc();
    
    // Find last 30 active users globally
    @Query(value = "SELECT * FROM user_history ORDER BY last_active DESC LIMIT 30", nativeQuery = true)
    List<UserHistory> findLast30ActiveUsers();
    
    // Find last 30 active users for a specific document
    @Query(value = "SELECT * FROM user_history WHERE document_id = :documentId ORDER BY last_active DESC LIMIT 30", nativeQuery = true)
    List<UserHistory> findLast30ActiveUsersForDocument(@Param("documentId") String documentId);
    
    // Find user's last activity
    Optional<UserHistory> findFirstByUserIdOrderByLastActiveDesc(String userId);
    
    // Find user's last activity in a specific document
    Optional<UserHistory> findFirstByUserIdAndDocumentIdOrderByLastActiveDesc(String userId, String documentId);
    
    // Count user activities in last 24 hours
    @Query(value = "SELECT COUNT(*) FROM user_history WHERE last_active >= :since", nativeQuery = true)
    Long countActiveUsersSince(@Param("since") LocalDateTime since);
    
    // Delete old user history records (cleanup)
    void deleteByLastActiveBefore(LocalDateTime cutoff);
    
    // Find the IDs of the last 30 active users
    @Query(value = "SELECT id FROM user_history ORDER BY last_active DESC LIMIT 30", nativeQuery = true)
    List<Long> findLast30UserHistoryIds();
    
    // Find the IDs of the last 50 active users
    @Query(value = "SELECT id FROM user_history ORDER BY last_active DESC LIMIT 50", nativeQuery = true)
    List<Long> findLast50UserHistoryIds();
    
    // Delete user history records by IDs
    @Modifying
    void deleteByIdIn(List<Long> ids);
    
    // Delete all records except those with specified IDs (more efficient for cleanup)
    @Modifying
    @Query(value = "DELETE FROM user_history WHERE id NOT IN (:ids)", nativeQuery = true)
    long deleteByIdNotIn(@Param("ids") List<Long> ids);
    
    // Find users active in a document since a specific time
    @Query(value = "SELECT DISTINCT user_id, display_name, user_color FROM user_history WHERE document_id = :documentId AND last_active >= :since ORDER BY last_active DESC", nativeQuery = true)
    List<Object[]> findActiveUsersInDocumentSince(@Param("documentId") String documentId, @Param("since") LocalDateTime since);
}
