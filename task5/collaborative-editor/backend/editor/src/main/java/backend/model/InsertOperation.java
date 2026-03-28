package backend.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.persistence.Column;

@JsonTypeName("insert")
public class InsertOperation extends Operation {
    
    @Column(name = "position")
    private Integer position;
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    // Constructors
    public InsertOperation() {
        super();
    }
    
    public InsertOperation(String documentId, String userId, Long timestamp, Integer sequenceNumber, Integer position, String content) {
        super(documentId, userId, timestamp, sequenceNumber);
        this.position = position;
        this.content = content;
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
    
    private InsertOperation transformInsert(InsertOperation other) {
        // If other operation was from same user and earlier timestamp, no transformation needed
        if (other.getUserId().equals(this.getUserId()) && other.getTimestamp() < this.getTimestamp()) {
            return this;
        }
        
        // If other insert is at or before our position, shift our position
        if (other.getPosition() <= this.position) {
            return new InsertOperation(
                this.getDocumentId(), this.getUserId(), this.getTimestamp(), 
                this.getSequenceNumber(), this.position + other.getContent().length(), 
                this.content
            );
        }
        
        return this;
    }
    
    private InsertOperation transformDelete(DeleteOperation other) {
        // If delete is before our position, shift our position left
        if (other.getPosition() < this.position) {
            int newPosition = this.position - Math.min(other.getLength(), this.position - other.getPosition());
            return new InsertOperation(
                this.getDocumentId(), this.getUserId(), this.getTimestamp(),
                this.getSequenceNumber(), newPosition, this.content
            );
        }
        
        return this;
    }
    
    private InsertOperation transformRetain(RetainOperation other) {
        // Retain doesn't affect insert position
        return this;
    }
    
    @Override
    public String apply(String content) {
        System.out.println("🔧 InsertOperation.apply() called");
        System.out.println("🔧 Input content: '" + content + "'");
        System.out.println("🔧 Insert position: " + position);
        System.out.println("🔧 Insert content: '" + this.content + "'");
        System.out.println("🔧 Content length: " + content.length());
        
        String result;
        if (position > content.length()) {
            result = content + this.content;
            System.out.println("🔧 Position > length, appending to end");
        } else {
            String before = content.substring(0, position);
            String after = content.substring(position);
            result = before + this.content + after;
            System.out.println("🔧 Before substring: '" + before + "'");
            System.out.println("🔧 After substring: '" + after + "'");
            System.out.println("🔧 Result: '" + result + "'");
        }
        
        return result;
    }
    
    @Override
    public int getLength() {
        return content.length();
    }
    
    @Override
    public String getType() {
        return "insert";
    }
    
    @Override
    public String toString() {
        return "insert[position=" + position + ",content='" + content + "']";
    }
    
    // Getters and Setters
    public int getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
