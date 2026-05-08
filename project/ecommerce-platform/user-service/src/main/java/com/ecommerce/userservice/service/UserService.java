package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.request.LoginRequest;
import com.ecommerce.userservice.dto.request.RegisterRequest;
import com.ecommerce.userservice.dto.request.UpdateProfileRequest;
import com.ecommerce.userservice.dto.response.AuthResponse;
import com.ecommerce.userservice.dto.response.UserResponse;
import com.ecommerce.common.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface UserService {

    AuthResponse register(RegisterRequest request, String requestId);

    AuthResponse login(LoginRequest request, String requestId);

    UserResponse getUserById(String userId, String requestId);

    UserResponse getUserByEmail(String email, String requestId);

    UserResponse updateProfile(String userId, UpdateProfileRequest request,
                                String requestId, String requestingUserId);

    PageResponse<UserResponse> getAllUsers(Pageable pageable, String requestId);

    PageResponse<UserResponse> searchUsers(String searchTerm, Pageable pageable, String requestId);

    void setUserEnabled(String userId, boolean enabled, String requestId);

    /**
     * Validates that a userId exists (used by other services for ID validation).
     *
     * @param userId    the user ID to check
     * @return true if user exists and is enabled
     */
    boolean userExistsAndEnabled(String userId);
}
