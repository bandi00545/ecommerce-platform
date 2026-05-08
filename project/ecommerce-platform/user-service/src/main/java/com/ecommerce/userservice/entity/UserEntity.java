package com.ecommerce.userservice.entity;

import com.ecommerce.common.entity.BaseEntity;
import com.ecommerce.common.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_email",    columnList = "email",    unique = true),
        @Index(name = "idx_users_username", columnList = "username", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = "password")  // Never include password in toString/logs
public class UserEntity extends BaseEntity {

    /**
     * User's full name for display purposes.
     * Not used for login - email is the login identifier.
     */
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /**
     * Unique username for display (e.g. john_doe).
     * Rules: 3-20 chars, alphanumeric + underscore only.
     * Indexed for fast lookup.
     */
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /**
     * Email address - used as login identifier.
     * Must be globally unique across all users.
     * Indexed for fast login lookup.
     */
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /**
     * BCrypt hashed password.
     * NEVER stored as plaintext.
     * BCrypt format: $2a$10$[22-char-salt][31-char-hash]
     * @JsonIgnore in response DTOs ensures it's never serialized.
     */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    /**
     * User's mobile phone number (Indian format: 10 digits).
     * Optional field.
     */
    @Column(name = "phone", length = 15)
    private String phone;

    /**
     * Role determines what the user can access.
     * USER = standard customer
     * ADMIN = platform administrator
     * Stored as VARCHAR for readability in DB.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.USER;

    /**
     * Whether the account is active.
     * Soft-disable: false = account cannot login but data is preserved.
     * Admin can re-enable by setting to true.
     */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /**
     * URL to the user's profile picture.
     * Stored as a path to object storage (S3/MinIO).
     * Optional field.
     */
    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

    /**
     * User's physical address (single-line format).
     * Optional field.
     */
    @Column(name = "address", length = 500)
    private String address;

    /**
     * Number of failed login attempts.
     * Used for account lockout after repeated failures.
     * Reset to 0 on successful login.
     */
    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;
}
