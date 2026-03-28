package backend.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.persistence.*;

@Entity
@Table(name = "operations")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public abstract class Operation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "document_id")
    private String documentId;
    
    @Column(name = "user_id")
    private String userId;
    
    @Column(name = "timestamp")
    private Long timestamp;
    
    @Column(name = "sequence_number")
    private Integer sequenceNumber;
    
    // Constructors
    public Operation() {}
    
    public Operation(String documentId, String userId, Long timestamp, Integer sequenceNumber) {
        this.documentId = documentId;
        this.userId = userId;
        this.timestamp = timestamp;
        this.sequenceNumber = sequenceNumber;
    }
    
    // Abstract methods for transformation
    public abstract Operation transform(Operation other);
    public abstract String apply(String content);
    public abstract int getLength();
    
    // Common methods for all operations
    public abstract String getType();
    public abstract int getPosition();
    
    @Override
    public String toString() {
        return getType() + "[position=" + getPosition() + "]";
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    
    public Integer getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(Integer sequenceNumber) { this.sequenceNumber = sequenceNumber; }
}
