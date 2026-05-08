package com.ecommerce.userservice.repository;

import com.ecommerce.userservice.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {

    Optional<UserEntity> findByEmail(String email);

    /**
     * Find user by username for display name lookups.
     *
     * @param username the username to search
     * @return Optional containing user if found
     */
    Optional<UserEntity> findByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * Check if a username already exists (for registration duplicate check).
     *
     * @param username the username to check
     * @return true if a user with this username exists
     */
    boolean existsByUsername(String username);

    Page<UserEntity> findAllByEnabled(boolean enabled, Pageable pageable);

    @Query("""
            SELECT u FROM UserEntity u
            WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
               OR LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            ORDER BY u.createdAt DESC
            """)
    Page<UserEntity> searchUsers(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Modifying
    @Query("UPDATE UserEntity u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.id = :userId")
    void incrementFailedLoginAttempts(@Param("userId") String userId);

    /**
     * Reset failed login attempts to 0 after successful login.
     *
     * @param userId the user's ID
     */
    @Modifying
    @Query("UPDATE UserEntity u SET u.failedLoginAttempts = 0 WHERE u.id = :userId")
    void resetFailedLoginAttempts(@Param("userId") String userId);

    @Modifying
    @Query("UPDATE UserEntity u SET u.enabled = :enabled WHERE u.id = :userId")
    void updateEnabledStatus(@Param("userId") String userId, @Param("enabled") boolean enabled);
}
