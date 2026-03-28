package backend.dto;

public class EditMessage {

    private String documentId;
    private String userId;
    private String displayName;
    private String operation;
    private int position;
    private int cursorPosition;
    private String text;
    private String targetUserId;
    private long timestamp;
    private boolean isEditing;
    private java.util.Map<String, String> metadata;

    // Getters and Setters

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }

    public int getCursorPosition() {
        return cursorPosition;
    }

    public void setCursorPosition(int cursorPosition) {
        this.cursorPosition = cursorPosition;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isEditing() {
        return isEditing;
    }

    public void setEditing(boolean editing) {
        isEditing = editing;
    }

    public java.util.Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(java.util.Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public void setMetadata(String key, String value) {
        if (this.metadata == null) {
            this.metadata = new java.util.HashMap<>();
        }
        this.metadata.put(key, value);
    }

    @Override
    public String toString() {
        return "EditMessage{" +
                "documentId='" + documentId + '\'' +
                ", userId='" + userId + '\'' +
                ", operation='" + operation + '\'' +
                ", position=" + position +
                ", cursorPosition=" + cursorPosition +
                ", text='" + text + '\'' +
                ", targetUserId='" + targetUserId + '\'' +
                ", timestamp=" + timestamp +
                ", isEditing=" + isEditing +
                '}';
    }
}
