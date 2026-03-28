package backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "content_history")
public class ContentHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "document_id", nullable = false)
    private String documentId;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "username")
    private String username;
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "content_length", nullable = false)
    private int contentLength;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "operation_type", nullable = false)
    private String operationType; // INSERT, UPDATE, DELETE
    
    @Column(name = "character_count")
    private Integer characterCount;
    
    // Constructors
    public ContentHistory() {
        this.createdAt = LocalDateTime.now();
        this.operationType = "UPDATE";
    }
    
    public ContentHistory(String documentId, String userId, String username, String content, String operationType) {
        this();
        this.documentId = documentId;
        this.userId = userId;
        this.username = username;
        this.content = content;
        this.operationType = operationType;
        this.contentLength = content != null ? content.length() : 0;
        this.characterCount = content != null ? content.length() : 0;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getContent() { return content; }
    public void setContent(String content) { 
        this.content = content;
        this.contentLength = content != null ? content.length() : 0;
        this.characterCount = content != null ? content.length() : 0;
    }
    
    public int getContentLength() { return contentLength; }
    public void setContentLength(int contentLength) { this.contentLength = contentLength; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    
    public Integer getCharacterCount() { return characterCount; }
    public void setCharacterCount(Integer characterCount) { this.characterCount = characterCount; }
    
    @Override
    public String toString() {
        return "ContentHistory{" +
                "id=" + id +
                ", documentId='" + documentId + '\'' +
                ", userId='" + userId + '\'' +
                ", contentLength=" + contentLength +
                ", createdAt=" + createdAt +
                ", operationType='" + operationType + '\'' +
                '}';
    }
}
