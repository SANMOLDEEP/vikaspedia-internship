package backend.repository;

import backend.model.DocumentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadata, String> {
    
    List<DocumentMetadata> findByOrderByUpdatedAtDesc();
    
    List<DocumentMetadata> findByCreatedByOrderByUpdatedAtDesc(String createdBy);
    
    Optional<DocumentMetadata> findByName(String name);
    
    @Query("SELECT d FROM DocumentMetadata d WHERE d.name LIKE %:name% ORDER BY d.updatedAt DESC")
    List<DocumentMetadata> findByNameContainingIgnoreCase(String name);
    
    boolean existsByName(String name);
}
