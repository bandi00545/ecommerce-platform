package com.ecommerce.userservice.service.impl;

import com.ecommerce.common.constants.AppConstants;
import com.ecommerce.common.dto.response.PageResponse;
import com.ecommerce.common.enums.AuditStatus;
import com.ecommerce.common.enums.ErrorCode;
import com.ecommerce.common.enums.UserRole;
import com.ecommerce.common.exception.DuplicateResourceException;
import com.ecommerce.common.exception.ForbiddenException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.exception.UnauthorizedException;
import com.ecommerce.common.exception.ValidationException;
import com.ecommerce.userservice.dto.request.LoginRequest;
import com.ecommerce.userservice.dto.request.RegisterRequest;
import com.ecommerce.userservice.dto.request.UpdateProfileRequest;
import com.ecommerce.userservice.dto.response.AuthResponse;
import com.ecommerce.userservice.dto.response.UserResponse;
import com.ecommerce.userservice.entity.UserEntity;
import com.ecommerce.userservice.mapper.UserMapper;
import com.ecommerce.userservice.repository.UserRepository;
import com.ecommerce.userservice.service.AuditEventPublisher;
import com.ecommerce.userservice.service.JwtService;
import com.ecommerce.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository    userRepository;
    private final PasswordEncoder   passwordEncoder;
    private final JwtService        jwtService;
    private final UserMapper        userMapper;
    private final AuditEventPublisher auditPublisher;

    // Max failed login attempts before we log a warning (can add lockout logic here)
    private static final int MAX_FAILED_ATTEMPTS = 5;

    // =========================================================================
    // REGISTER
    // =========================================================================

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, String requestId) {
        log.info("Register request | requestId={} | email={}", requestId, request.getEmail());

        // 1. Check email uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: email exists | requestId={} | email={}",
                    requestId, request.getEmail());
            auditPublisher.publishAuditEvent(requestId, null,
                    AppConstants.ACTION_REGISTER, AuditStatus.FAILURE,
                    "Registration failed: email already registered: " + request.getEmail(),
                    "User", null, "EMAIL_ALREADY_EXISTS");
            throw new DuplicateResourceException(
                    ErrorCode.USER_ALREADY_EXISTS,
                    "A user with email '" + request.getEmail() + "' already exists"
            );
        }

        // 2. Check username uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed: username taken | requestId={} | username={}",
                    requestId, request.getUsername());
            throw new DuplicateResourceException(
                    ErrorCode.USERNAME_TAKEN,
                    "Username '" + request.getUsername() + "' is already taken"
            );
        }

        // 3. Map request to entity (MapStruct - compile-time generated)
        UserEntity user = userMapper.toEntity(request);

        // 4. Hash password with BCrypt (strength 10 = ~100ms per hash)
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // 5. Set defaults explicitly (belt and suspenders)
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        user.setFailedLoginAttempts(0);

        // 6. Persist to DB
        UserEntity savedUser = userRepository.save(user);
        log.info("User registered successfully | requestId={} | userId={} | email={}",
                requestId, savedUser.getId(), savedUser.getEmail());

        // 7. Publish success audit event (async - non-blocking)
        auditPublisher.publishAuditEvent(requestId, savedUser.getId(),
                AppConstants.ACTION_REGISTER, AuditStatus.SUCCESS,
                "New user registered: " + savedUser.getEmail(),
                "User", savedUser.getId(), null);

        // 8. Generate JWT and return AuthResponse (auto-login after register)
        return buildAuthResponse(savedUser);
    }

    // =========================================================================
    // LOGIN
    // =========================================================================

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String requestId) {
        log.info("Login attempt | requestId={} | email={}", requestId, request.getEmail());

        // 1. Find user by email
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    // Use generic message to prevent email enumeration attacks
                    auditPublisher.publishAuditEvent(requestId, null,
                            AppConstants.ACTION_LOGIN, AuditStatus.FAILURE,
                            "Login failed: user not found for email " + request.getEmail(),
                            "User", null, "USER_NOT_FOUND");
                    return new UnauthorizedException(
                            ErrorCode.INVALID_CREDENTIALS, "Invalid email or password"
                    );
                });

        // 2. Check account is enabled
        if (!user.isEnabled()) {
            log.warn("Login attempt on disabled account | requestId={} | userId={}",
                    requestId, user.getId());
            auditPublisher.publishAuditEvent(requestId, user.getId(),
                    AppConstants.ACTION_LOGIN, AuditStatus.FAILURE,
                    "Login failed: account is disabled",
                    "User", user.getId(), "ACCOUNT_DISABLED");
            throw new UnauthorizedException(
                    ErrorCode.ACCOUNT_DISABLED,
                    "This account has been disabled. Please contact support."
            );
        }

        // 3. Verify password against BCrypt hash
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // Increment failed attempts counter atomically
            userRepository.incrementFailedLoginAttempts(user.getId());

            int attempts = user.getFailedLoginAttempts() + 1;
            log.warn("Login failed: wrong password | requestId={} | userId={} | attempts={}",
                    requestId, user.getId(), attempts);

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                log.warn("SECURITY: Too many failed login attempts | userId={}", user.getId());
                // In production: lock account after MAX_FAILED_ATTEMPTS
                // userRepository.updateEnabledStatus(user.getId(), false);
            }

            auditPublisher.publishAuditEvent(requestId, user.getId(),
                    AppConstants.ACTION_LOGIN, AuditStatus.FAILURE,
                    "Login failed: invalid password. Attempt #" + attempts,
                    "User", user.getId(), "INVALID_PASSWORD");

            throw new UnauthorizedException(
                    ErrorCode.INVALID_CREDENTIALS, "Invalid email or password"
            );
        }

        // 4. Successful login - reset failed attempts counter
        userRepository.resetFailedLoginAttempts(user.getId());

        log.info("Login successful | requestId={} | userId={} | role={}",
                requestId, user.getId(), user.getRole());

        // 5. Publish success audit
        auditPublisher.publishAuditEvent(requestId, user.getId(),
                AppConstants.ACTION_LOGIN, AuditStatus.SUCCESS,
                "User logged in successfully",
                "User", user.getId(), null);

        // 6. Generate and return JWT
        return buildAuthResponse(user);
    }

    // =========================================================================
    // GET USER BY ID (with Redis caching)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "'user:' + #userId", unless = "#result == null")
    public UserResponse getUserById(String userId, String requestId) {
        log.debug("Fetching user | requestId={} | userId={}", requestId, userId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.USER_NOT_FOUND,
                        "User not found with id: " + userId
                ));

        return userMapper.toResponse(user);
    }

    // =========================================================================
    // GET USER BY EMAIL
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email, String requestId) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.USER_NOT_FOUND,
                        "User not found with email: " + email
                ));
        return userMapper.toResponse(user);
    }

    // =========================================================================
    // UPDATE PROFILE
    // =========================================================================

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "'user:' + #userId")
    public UserResponse updateProfile(String userId, UpdateProfileRequest request,
                                       String requestId, String requestingUserId) {
        log.info("Update profile | requestId={} | userId={} | requestingUser={}",
                requestId, userId, requestingUserId);

        // Authorization: only self or ADMIN can update
        if (!userId.equals(requestingUserId)) {
            // Check if requesting user is admin (fetch their role)
            UserEntity requestingUser = userRepository.findById(requestingUserId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User", "id", requestingUserId));
            if (requestingUser.getRole() != UserRole.ADMIN) {
                throw new ForbiddenException(
                        "You can only update your own profile"
                );
            }
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.USER_NOT_FOUND, "User not found with id: " + userId
                ));

        // Apply only non-null fields (PATCH semantics via MapStruct NullValuePropertyMappingStrategy.IGNORE)
        userMapper.updateEntityFromRequest(request, user);

        UserEntity updated = userRepository.save(user);

        log.info("Profile updated | requestId={} | userId={}", requestId, userId);

        auditPublisher.publishAuditEvent(requestId, requestingUserId,
                AppConstants.ACTION_UPDATE, AuditStatus.SUCCESS,
                "Profile updated for user: " + userId,
                "User", userId, null);

        return userMapper.toResponse(updated);
    }

    // =========================================================================
    // GET ALL USERS (Admin)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(Pageable pageable, String requestId) {
        log.debug("Admin: listing all users | requestId={} | page={} | size={}",
                requestId, pageable.getPageNumber(), pageable.getPageSize());

        Page<UserEntity> usersPage = userRepository.findAll(pageable);
        return PageResponse.from(usersPage, userMapper::toResponse);
    }

    // =========================================================================
    // SEARCH USERS (Admin)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> searchUsers(String searchTerm, Pageable pageable,
                                                   String requestId) {
        log.debug("Admin: searching users | requestId={} | term={}", requestId, searchTerm);

        Page<UserEntity> results = userRepository.searchUsers(searchTerm, pageable);
        return PageResponse.from(results, userMapper::toResponse);
    }

    // =========================================================================
    // ENABLE / DISABLE USER (Admin)
    // =========================================================================

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "'user:' + #userId")
    public void setUserEnabled(String userId, boolean enabled, String requestId) {
        log.info("Admin: {} user | requestId={} | userId={}",
                enabled ? "enabling" : "disabling", requestId, userId);

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND,
                    "User not found with id: " + userId);
        }

        userRepository.updateEnabledStatus(userId, enabled);

        auditPublisher.publishAuditEvent(requestId, null,
                AppConstants.ACTION_UPDATE, AuditStatus.SUCCESS,
                "User account " + (enabled ? "enabled" : "disabled") + ": " + userId,
                "User", userId, null);
    }

    // =========================================================================
    // USER EXISTS CHECK (used by other services)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public boolean userExistsAndEnabled(String userId) {
        return userRepository.findById(userId)
                .map(user -> user.isEnabled())
                .orElse(Boolean.FALSE);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Builds an AuthResponse from a UserEntity by generating a JWT token.
     */
    private AuthResponse buildAuthResponse(UserEntity user) {
        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresInMinutes(jwtService.getExpirationMinutes())
                .expiresAt(jwtService.getTokenExpiryDateTime())
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .fullName(user.getFullName())
                .build();
    }
}
