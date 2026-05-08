package com.ecommerce.userservice.facade;

import com.ecommerce.common.constants.AppConstants;
import com.ecommerce.common.context.RequestContext;
import com.ecommerce.common.dto.response.PageResponse;
import com.ecommerce.common.enums.UserRole;
import com.ecommerce.common.exception.ForbiddenException;
import com.ecommerce.userservice.dto.request.LoginRequest;
import com.ecommerce.userservice.dto.request.RegisterRequest;
import com.ecommerce.userservice.dto.request.UpdateProfileRequest;
import com.ecommerce.userservice.dto.response.AuthResponse;
import com.ecommerce.userservice.dto.response.UserResponse;
import com.ecommerce.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserFacade {

    private final UserService userService;

    // =========================================================================
    // AUTH
    // =========================================================================

    public AuthResponse register(RegisterRequest request) {
        String requestId = RequestContext.getRequestIdSafe();
        log.debug("Facade: register | requestId={} | email={}", requestId, request.getEmail());
        return userService.register(request, requestId);
    }

    public AuthResponse login(LoginRequest request) {
        String requestId = RequestContext.getRequestIdSafe();
        log.debug("Facade: login | requestId={} | email={}", requestId, request.getEmail());
        return userService.login(request, requestId);
    }

    // =========================================================================
    // USER PROFILE
    // =========================================================================

    public UserResponse getMyProfile() {
        String requestId = RequestContext.getRequestIdSafe();
        String userId    = RequestContext.getUserId();
        log.debug("Facade: getMyProfile | requestId={} | userId={}", requestId, userId);
        return userService.getUserById(userId, requestId);
    }

    public UserResponse getUserById(String userId) {
        String requestId       = RequestContext.getRequestIdSafe();
        String requestingUserId = RequestContext.getUserId();
        String requestingRole  = RequestContext.getUserRole();

        // Authorization: users can only get their own profile; admins can get any
        if (!userId.equals(requestingUserId) && !UserRole.ADMIN.name().equals(requestingRole)) {
            throw new ForbiddenException("You can only view your own profile", requestId);
        }

        return userService.getUserById(userId, requestId);
    }

    public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
        String requestId        = RequestContext.getRequestIdSafe();
        String requestingUserId = RequestContext.getUserId();
        return userService.updateProfile(userId, request, requestId, requestingUserId);
    }

    public UserResponse updateMyProfile(UpdateProfileRequest request) {
        String requestId = RequestContext.getRequestIdSafe();
        String userId    = RequestContext.getUserId();
        return userService.updateProfile(userId, request, requestId, userId);
    }

    // =========================================================================
    // ADMIN OPERATIONS
    // =========================================================================

    public PageResponse<UserResponse> getAllUsers(int page, int size, String sortBy, String sortDir) {
        String requestId = RequestContext.getRequestIdSafe();
        requireAdminRole();

        // Validate and cap page size
        size = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return userService.getAllUsers(pageable, requestId);
    }

    public PageResponse<UserResponse> searchUsers(String searchTerm, int page, int size) {
        String requestId = RequestContext.getRequestIdSafe();
        requireAdminRole();

        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return userService.searchUsers(searchTerm, pageable, requestId);
    }

    public void enableUser(String userId) {
        requireAdminRole();
        userService.setUserEnabled(userId, true, RequestContext.getRequestIdSafe());
    }

    public void disableUser(String userId) {
        requireAdminRole();
        userService.setUserEnabled(userId, false, RequestContext.getRequestIdSafe());
    }

    // =========================================================================
    // PRIVATE
    // =========================================================================

    /**
     * Throws ForbiddenException if the current user is not ADMIN.
     * Called at the start of all admin-only facade methods.
     */
    private void requireAdminRole() {
        String role      = RequestContext.getUserRole();
        String requestId = RequestContext.getRequestIdSafe();
        if (!UserRole.ADMIN.name().equals(role)) {
            throw new ForbiddenException(
                    "This operation requires ADMIN role", requestId
            );
        }
    }
}
