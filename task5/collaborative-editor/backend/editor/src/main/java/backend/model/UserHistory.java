package backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_history")
public class UserHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "display_name")
    private String displayName;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "avatar_url")
    private String avatarUrl;
    
    @Column(name = "user_color")
    private String userColor;
    
    @Column(name = "is_guest")
    private Boolean isGuest = true;
    
    @Column(name = "document_id")
    private String documentId;
    
    @Column(name = "activity_type")
    private String activityType = "ACTIVE";
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "last_active")
    private LocalDateTime lastActive;
    
    // Constructors
    public UserHistory() {}
    
    public UserHistory(String userId, String displayName, String documentId, String activityType) {
        this.userId = userId;
        this.displayName = displayName;
        this.documentId = documentId;
        this.activityType = activityType;
        this.createdAt = LocalDateTime.now();
        this.lastActive = LocalDateTime.now();
    }
    
    public UserHistory(String userId, String displayName, String email, String documentId, String activityType) {
        this.userId = userId;
        this.displayName = displayName;
        this.email = email;
        this.documentId = documentId;
        this.activityType = activityType;
        this.createdAt = LocalDateTime.now();
        this.lastActive = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    
    public String getUserColor() {
        return userColor;
    }
    
    public void setUserColor(String userColor) {
        this.userColor = userColor;
    }
    
    public Boolean getIsGuest() {
        return isGuest;
    }
    
    public void setIsGuest(Boolean isGuest) {
        this.isGuest = isGuest;
    }
    
    public String getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    
    public String getActivityType() {
        return activityType;
    }
    
    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastActive() {
        return lastActive;
    }
    
    public void setLastActive(LocalDateTime lastActive) {
        this.lastActive = lastActive;
    }
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (lastActive == null) {
            lastActive = LocalDateTime.now();
        }
    }
}
