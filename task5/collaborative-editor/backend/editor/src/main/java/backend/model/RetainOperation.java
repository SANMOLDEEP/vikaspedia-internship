package backend.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.persistence.Column;

@JsonTypeName("retain")
public class RetainOperation extends Operation {
    
    @Column(name = "length")
    private Integer length;
    
    // Constructors
    public RetainOperation() {
        super();
    }
    
    public RetainOperation(String documentId, String userId, Long timestamp, Integer sequenceNumber, Integer length) {
        super(documentId, userId, timestamp, sequenceNumber);
        this.length = length;
    }
    
    @Override
    public Operation transform(Operation other) {
        // Retain operations don't affect other operations in basic OT
        return other;
    }
    
    @Override
    public String apply(String content) {
        // Retain operation doesn't modify content
        return content;
    }
    
    @Override
    public int getLength() {
        return length;
    }
    
    @Override
    public String getType() {
        return "retain";
    }
    
    @Override
    public int getPosition() {
        return 0; // Retain operations don't have a position
    }
    
    // Getters and Setters
    public void setLength(Integer length) { this.length = length; }
}
