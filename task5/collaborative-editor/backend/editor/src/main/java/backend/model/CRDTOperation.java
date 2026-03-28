package backend.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.io.IOException;

@JsonTypeName("crdt")
@Entity
@DiscriminatorValue("CRDT")
public class CRDTOperation extends Operation {
    
    @Column(name = "char_id")
    private String charId;
    
    @Column(name = "content")
    private String content;
    
    @Column(name = "priority")
    private Double priority;
    
    // Constructors
    public CRDTOperation() {
        super();
    }
    
    public CRDTOperation(String documentId, String userId, Long timestamp, 
                        Integer sequenceNumber, String charId, String content, Double priority) {
        super(documentId, userId, timestamp, sequenceNumber);
        this.charId = charId;
        this.content = content;
        this.priority = priority;
    }
    
    @Override
    public Operation transform(Operation other) {
        // CRDT operations are commutative - no transformation needed
        // All CRDT operations can be applied in any order
        System.out.println("🔧 CRDTOperation.transform() called - no transformation needed (commutative)");
        return this;
    }
    
    @Override
    public String apply(String currentContent) {
        System.out.println("🔧 CRDTOperation.apply() called");
        System.out.println("🔧 Char ID: " + charId);
        System.out.println("🔧 Content: '" + content + "'");
        System.out.println("🔧 Priority: " + priority);
        System.out.println("🔧 Current content: '" + currentContent + "'");
        
        // CRDT merge: preserve spaces and newlines exactly
        if (content == null || content.isEmpty()) {
            System.out.println("🔧 No content to apply, returning current content");
            return currentContent; // No content to insert
        }
        
        // For CRDT, we'll use the new content directly (since it's already merged)
        // This preserves spaces, newlines, and all characters exactly
        String result = content;
        System.out.println("🔧 CRDT Result: '" + result + "'");
        
        return result;
    }
    
    @Override
    public int getLength() {
        return content != null ? content.length() : 0;
    }
    
    @Override
    public String getType() {
        return "crdt";
    }
    
    @Override
    public int getPosition() {
        // CRDT operations don't have traditional positions
        // Return 0 as default for compatibility
        return 0;
    }
    
    @Override
    public String toString() {
        return "crdt[charId=" + charId + ",content='" + content + "',priority=" + priority + "]";
    }
    
    // Getters and Setters
    public String getCharId() { return charId; }
    public void setCharId(String charId) { this.charId = charId; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public Double getPriority() { return priority; }
    public void setPriority(Double priority) { this.priority = priority; }
    
    // Add missing methods for CRDT service
    public String getCharacterId() { return charId; }
    
    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (IOException e) {
            return "{\"error\":\"Failed to serialize CRDTOperation\"}";
        }
    }
    
    public static CRDTOperation fromJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, CRDTOperation.class);
        } catch (IOException e) {
            // Return a default operation if parsing fails
            CRDTOperation defaultOp = new CRDTOperation();
            defaultOp.setCharId("error");
            defaultOp.setContent("error");
            defaultOp.setPriority(0.0);
            return defaultOp;
        }
    }
}
