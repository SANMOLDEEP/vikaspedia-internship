package backend.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.persistence.Column;

@JsonTypeName("delete")
public class DeleteOperation extends Operation {
    
    @Column(name = "position")
    private Integer position;
    
    @Column(name = "length")
    private Integer length;
    
    // Constructors
    public DeleteOperation() {
        super();
    }
    
    public DeleteOperation(String documentId, String userId, Long timestamp, Integer sequenceNumber, Integer position, Integer length) {
        super(documentId, userId, timestamp, sequenceNumber);
        this.position = position;
        this.length = length;
    }
    
    @Override
    public Operation transform(Operation other) {
        if (other instanceof InsertOperation) {
            return transformInsert((InsertOperation) other);
        } else if (other instanceof DeleteOperation) {
            return transformDelete((DeleteOperation) other);
        } else if (other instanceof RetainOperation) {
            return transformRetain((RetainOperation) other);
        }
        return this;
    }
    
    private DeleteOperation transformInsert(InsertOperation other) {
        // If other operation was from same user and earlier timestamp, no transformation needed
        if (other.getUserId().equals(this.getUserId()) && other.getTimestamp() < this.getTimestamp()) {
            return this;
        }
        
        // If other insert is before our delete position, shift our delete position right
        if (other.getPosition() <= this.position) {
            return new DeleteOperation(
                this.getDocumentId(), this.getUserId(), this.getTimestamp(),
                this.getSequenceNumber(), this.position + other.getContent().length(),
                this.length
            );
        }
        
        // If other insert is within our delete range, extend our delete length
        if (other.getPosition() < this.position + this.length) {
            return new DeleteOperation(
                this.getDocumentId(), this.getUserId(), this.getTimestamp(),
                this.getSequenceNumber(), this.position,
                this.length + other.getContent().length()
            );
        }
        
        return this;
    }
    
    private DeleteOperation transformDelete(DeleteOperation other) {
        // If other operation was from same user and earlier timestamp, no transformation needed
        if (other.getUserId().equals(this.getUserId()) && other.getTimestamp() < this.getTimestamp()) {
            return this;
        }
        
        // If other delete is completely before our delete, shift our position left
        if (other.getPosition() + other.getLength() <= this.position) {
            return new DeleteOperation(
                this.getDocumentId(), this.getUserId(), this.getTimestamp(),
                this.getSequenceNumber(), this.position - other.getLength(),
                this.length
            );
        }
        
        // If other delete overlaps with our delete, we need to adjust
        int otherEnd = other.getPosition() + other.getLength();
        int ourEnd = this.position + this.length;
        
        if (other.getPosition() < ourEnd && otherEnd > this.position) {
            // Calculate overlap
            int overlapStart = Math.max(this.position, other.getPosition());
            int overlapEnd = Math.min(ourEnd, otherEnd);
            int overlapLength = overlapEnd - overlapStart;
            
            // Reduce our delete length by the overlap
            return new DeleteOperation(
                this.getDocumentId(), this.getUserId(), this.getTimestamp(),
                this.getSequenceNumber(), this.position,
                this.length - overlapLength
            );
        }
        
        return this;
    }
    
    private DeleteOperation transformRetain(RetainOperation other) {
        // Retain doesn't affect delete position
        return this;
    }
    
    @Override
    public String apply(String content) {
        if (position >= content.length()) {
            return content;
        }
        
        int actualLength = Math.min(length, content.length() - position);
        return content.substring(0, position) + content.substring(position + actualLength);
    }
    
    @Override
    public int getLength() {
        return length;
    }
    
    @Override
    public String getType() {
        return "delete";
    }
    
    @Override
    public String toString() {
        return "delete[position=" + position + ",length=" + length + "]";
    }
    
    // Getters and Setters
    public int getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
    
    public void setLength(Integer length) { this.length = length; }
}
