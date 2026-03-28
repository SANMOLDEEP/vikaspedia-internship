package backend.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;

@JsonTypeName("compact")
public class CompactOperation {
    
    private String documentId;
    private String userId;
    private Long timestamp;
    private List<Change> changes;
    
    public static class Change {
        private ChangeType type;
        private int position;
        private String content;
        
        public Change() {}
        
        public Change(ChangeType type, int position, String content) {
            this.type = type;
            this.position = position;
            this.content = content;
        }
        
        public ChangeType getType() { return type; }
        public void setType(ChangeType type) { this.type = type; }
        
        public int getPosition() { return position; }
        public void setPosition(int position) { this.position = position; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
    
    public enum ChangeType {
        INSERT, DELETE
    }
    
    // Getters and Setters
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    
    public List<Change> getChanges() { return changes; }
    public void setChanges(List<Change> changes) { this.changes = changes; }
}
