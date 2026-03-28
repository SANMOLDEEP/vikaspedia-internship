package backend.repository;

import backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUserId(String userId);
    
    List<User> findByIsGuest(Boolean isGuest);
    
    @Query("SELECT u FROM User u WHERE u.lastActive >= :since ORDER BY u.lastActive DESC")
    List<User> findActiveUsersSince(@Param("since") java.time.LocalDateTime since);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.isGuest = false")
    long countRegisteredUsers();
    
    @Modifying
    @Query("DELETE FROM User u WHERE u.isGuest = true AND u.lastActive < :cutoff")
    void deleteByIsGuestAndLastActiveBefore(@Param("cutoff") java.time.LocalDateTime cutoff);
    
    @Modifying
    @Query("DELETE FROM User u WHERE u.lastActive < :cutoff")
    int deleteByLastActiveBefore(@Param("cutoff") java.time.LocalDateTime cutoff);
    
    @Query("SELECT u FROM User u WHERE u.lastActive > :since ORDER BY u.lastActive DESC")
    List<User> findByLastActiveGreaterThanOrderByLastActiveDesc(@Param("since") java.time.LocalDateTime since);
    
    @Modifying
    @Query("DELETE FROM User u WHERE u.userId = :userId")
    void deleteByUserId(@Param("userId") String userId);
}
