package backend.service;

import backend.model.User;
import backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get or create user by ID
     */
    public User getOrCreateUser(String userId, String displayName) {
        Optional<User> existingUser = userRepository.findByUserId(userId);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // Update display name if it's different or null
            if (displayName != null && !displayName.trim().isEmpty() && 
                (user.getDisplayName() == null || !user.getDisplayName().equals(displayName))) {
                user.setDisplayName(displayName);
                System.out.println("🔄 Updated display name for user " + userId + " to: " + displayName);
            }
            user.updateLastActive();
            return userRepository.save(user);
        } else {
            // Create new guest user
            User newUser = new User(userId, displayName);
            System.out.println("🆕 Created new user " + userId + " with display name: " + displayName);
            return userRepository.save(newUser);
        }
    }
    
    /**
     * Register a new user (non-guest)
     */
    public User registerUser(String userId, String displayName, String email, String avatarUrl) {
        User user = new User(userId, displayName);
        user.setEmail(email);
        user.setAvatarUrl(avatarUrl);
        user.setIsGuest(false);
        user.setUserColor(generateUserColor(userId)); // Consistent color for registered users
        
        return userRepository.save(user);
    }
    
    /**
     * Generate consistent color for registered users based on user ID
     */
    private String generateUserColor(String userId) {
        String[] colors = {
            "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", 
            "#FFEAA7", "#DDA0DD", "#98D8C8", "#F7DC6F",
            "#BB8FCE", "#85C1E2", "#F8B739", "#52C777"
        };
        
        int hash = userId.hashCode();
        int index = Math.abs(hash) % colors.length;
        return colors[index];
    }
    
    /**
     * Find recently active users
     */
    public List<User> findRecentlyActiveUsers(int minutes) {
        return userRepository.findByLastActiveGreaterThanOrderByLastActiveDesc(
            java.time.LocalDateTime.now().minusMinutes(minutes)
        );
    }
    
    /**
     * Save user (wrapper for repository save)
     */
    public User saveUser(User user) {
        return userRepository.save(user);
    }
    
    /**
     * Delete a specific user
     */
    public void deleteUser(String userId) {
        userRepository.deleteByUserId(userId);
    }
    
    /**
     * Clean up inactive users
     */
    public int cleanupInactiveUsers(double minutesThreshold) {
        // Delete users inactive for more than the specified minutes
        return userRepository.deleteByLastActiveBefore(
            java.time.LocalDateTime.now().minusMinutes((long) minutesThreshold)
        );
    }
    
    /**
     * Update user profile
     */
    public User updateUserProfile(String userId, String displayName, String email, String avatarUrl) {
        Optional<User> existingUser = userRepository.findByUserId(userId);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setDisplayName(displayName);
            user.setEmail(email);
            user.setAvatarUrl(avatarUrl);
            user.updateLastActive();
            return userRepository.save(user);
        }
        
        throw new RuntimeException("User not found: " + userId);
    }
    
    /**
     * Get user by ID
     */
    public Optional<User> getUser(String userId) {
        return userRepository.findByUserId(userId);
    }
    
    /**
     * Get all active users
     */
    public List<User> getActiveUsers() {
        return userRepository.findActiveUsersSince(LocalDateTime.now().minusMinutes(5));
    }
    
    /**
     * Cleanup old guest users (older than 24 hours)
     */
    public void cleanupOldGuestUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        userRepository.deleteByIsGuestAndLastActiveBefore(cutoff);
    }
    
    /**
     * Get user info for display
     */
    public User getUserInfo(String userId) {
        Optional<User> user = userRepository.findByUserId(userId);
        if (user.isPresent()) {
            return user.get();
        }
        
        // Return default guest user if not found
        User defaultUser = new User(userId, userId);
        defaultUser.setIsGuest(true);
        return defaultUser;
    }
}
