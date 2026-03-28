package backend.repository;

import backend.model.ContentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContentHistoryRepository extends JpaRepository<ContentHistory, Long> {
    
    List<ContentHistory> findByDocumentIdOrderByCreatedAtDesc(String documentId);
    
    List<ContentHistory> findByDocumentIdOrderByCreatedAtAsc(String documentId);
    
    Optional<ContentHistory> findFirstByDocumentIdOrderByCreatedAtDesc(String documentId);
    
    @Query("SELECT ch FROM ContentHistory ch WHERE ch.documentId = ?1 AND ch.createdAt >= ?2 ORDER BY ch.createdAt ASC")
    List<ContentHistory> findByDocumentIdAndCreatedAtAfter(String documentId, LocalDateTime timestamp);
    
    @Query("SELECT COUNT(ch) FROM ContentHistory ch WHERE ch.documentId = ?1")
    long countByDocumentId(String documentId);
    
    @Query("SELECT ch FROM ContentHistory ch WHERE ch.documentId = ?1 ORDER BY ch.createdAt DESC LIMIT 1")
    Optional<ContentHistory> findLatestContent(String documentId);
    
    void deleteByDocumentId(String documentId);
    
    @Query("SELECT ch FROM ContentHistory ch WHERE ch.createdAt < ?1")
    List<ContentHistory> findOlderThan(LocalDateTime cutoff);
    
    // Find the IDs of the last 20 content history records per document
    @Query(value = "SELECT id FROM content_history WHERE document_id = :documentId ORDER BY created_at DESC LIMIT 20", nativeQuery = true)
    List<Long> findLast20ContentHistoryIdsByDocument(@Param("documentId") String documentId);
    
    // Find the IDs of the last 50 content history records per document
    @Query(value = "SELECT id FROM content_history WHERE document_id = :documentId ORDER BY created_at DESC LIMIT 50", nativeQuery = true)
    List<Long> findLast50ContentHistoryIdsByDocument(@Param("documentId") String documentId);
    
    // Find all distinct document IDs
    @Query("SELECT DISTINCT ch.documentId FROM ContentHistory ch")
    List<String> findAllDistinctDocumentIds();
    
    // Delete content history records by IDs
    @Modifying
    void deleteByIdIn(List<Long> ids);
}
