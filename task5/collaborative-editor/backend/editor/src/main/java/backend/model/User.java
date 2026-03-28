package backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", unique = true, nullable = false)
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
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "last_active")
    private LocalDateTime lastActive;
    
    // Constructors
    public User() {
        this.createdAt = LocalDateTime.now();
        this.lastActive = LocalDateTime.now();
    }
    
    public User(String userId, String displayName) {
        this();
        this.userId = userId;
        this.displayName = displayName;
        this.userColor = generateRandomColor();
    }
    
    // Generate random color for guest users
    private String generateRandomColor() {
        String[] colors = {
            "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", 
            "#FFEAA7", "#DDA0DD", "#98D8C8", "#F7DC6F",
            "#BB8FCE", "#85C1E2", "#F8B739", "#52C777"
        };
        return colors[(int) (Math.random() * colors.length)];
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getDisplayName() { return displayName != null ? displayName : userId; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    
    public String getUserColor() { return userColor; }
    public void setUserColor(String userColor) { this.userColor = userColor; }
    
    public Boolean getIsGuest() { return isGuest; }
    public void setIsGuest(Boolean isGuest) { this.isGuest = isGuest; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastActive() { return lastActive; }
    public void setLastActive(LocalDateTime lastActive) { this.lastActive = lastActive; }
    
    public void updateLastActive() {
        this.lastActive = LocalDateTime.now();
    }
}
