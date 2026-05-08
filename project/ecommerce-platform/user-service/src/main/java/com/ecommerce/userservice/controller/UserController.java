package com.ecommerce.userservice.controller;

import com.ecommerce.common.constants.AppConstants;
import com.ecommerce.common.context.RequestContext;
import com.ecommerce.common.dto.request.RequestEnvelope;
import com.ecommerce.common.dto.response.PageResponse;
import com.ecommerce.common.dto.response.ResponseEnvelope;
import com.ecommerce.userservice.dto.request.UpdateProfileRequest;
import com.ecommerce.userservice.dto.response.UserResponse;
import com.ecommerce.userservice.facade.UserFacade;
import com.ecommerce.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserFacade   userFacade;
    private final UserService  userService;

    /**
     * GET /api/v1/users/me
     * Returns the profile of the currently authenticated user.
     */
    @GetMapping("/me")
    public ResponseEntity<ResponseEnvelope<UserResponse>> getMyProfile() {
        String requestId = RequestContext.getRequestIdSafe();
        UserResponse user = userFacade.getMyProfile();
        return ResponseEntity.ok(ResponseEnvelope.success(
                user, AppConstants.MSG_FETCHED, requestId
        ));
    }

    /**
     * PUT /api/v1/users/me
     * Updates the currently authenticated user's profile.
     * Only non-null fields in the payload are updated (PATCH semantics).
     */
    @PutMapping("/me")
    public ResponseEntity<ResponseEnvelope<UserResponse>> updateMyProfile(
            @Valid @RequestBody RequestEnvelope<UpdateProfileRequest> envelope) {

        String requestId = RequestContext.getRequestIdSafe();
        UserResponse updated = userFacade.updateMyProfile(envelope.getPayload());
        return ResponseEntity.ok(ResponseEnvelope.success(
                updated, AppConstants.MSG_UPDATED, requestId
        ));
    }

    /**
     * GET /api/v1/users/{userId}
     * Returns a user's profile by ID.
     * Users can only fetch their own profile; ADMINs can fetch anyone.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ResponseEnvelope<UserResponse>> getUserById(
            @PathVariable String userId) {

        String requestId = RequestContext.getRequestIdSafe();
        UserResponse user = userFacade.getUserById(userId);
        return ResponseEntity.ok(ResponseEnvelope.success(
                user, AppConstants.MSG_FETCHED, requestId
        ));
    }

    /**
     * PUT /api/v1/users/{userId}
     * Admin-only: update any user's profile.
     */
    @PutMapping("/{userId}")
    public ResponseEntity<ResponseEnvelope<UserResponse>> updateUserProfile(
            @PathVariable String userId,
            @Valid @RequestBody RequestEnvelope<UpdateProfileRequest> envelope) {

        String requestId = RequestContext.getRequestIdSafe();
        UserResponse updated = userFacade.updateProfile(userId, envelope.getPayload());
        return ResponseEntity.ok(ResponseEnvelope.success(
                updated, AppConstants.MSG_UPDATED, requestId
        ));
    }

    /**
     * GET /api/v1/users?page=0&size=10&sortBy=createdAt&sortDir=desc
     * Admin-only: paginated list of all users.
     */
    @GetMapping
    public ResponseEntity<ResponseEnvelope<PageResponse<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE)   int size,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_BY)     String sortBy,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIR)    String sortDir) {

        String requestId = RequestContext.getRequestIdSafe();
        PageResponse<UserResponse> response = userFacade.getAllUsers(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ResponseEnvelope.success(
                response, AppConstants.MSG_FETCHED, requestId
        ));
    }

    /**
     * GET /api/v1/users/search?q=john&page=0&size=10
     * Admin-only: search users by name, email, or username.
     */
    @GetMapping("/search")
    public ResponseEntity<ResponseEnvelope<PageResponse<UserResponse>>> searchUsers(
            @RequestParam("q") String searchTerm,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        String requestId = RequestContext.getRequestIdSafe();
        PageResponse<UserResponse> response = userFacade.searchUsers(searchTerm, page, size);
        return ResponseEntity.ok(ResponseEnvelope.success(
                response, AppConstants.MSG_FETCHED, requestId
        ));
    }

    /**
     * PATCH /api/v1/users/{userId}/enable
     * Admin-only: enable a user account.
     */
    @PatchMapping("/{userId}/enable")
    public ResponseEntity<ResponseEnvelope<Void>> enableUser(@PathVariable String userId) {
        String requestId = RequestContext.getRequestIdSafe();
        userFacade.enableUser(userId);
        return ResponseEntity.ok(ResponseEnvelope.successMessage(
                "User account enabled successfully", requestId
        ));
    }

    /**
     * PATCH /api/v1/users/{userId}/disable
     * Admin-only: disable a user account.
     */
    @PatchMapping("/{userId}/disable")
    public ResponseEntity<ResponseEnvelope<Void>> disableUser(@PathVariable String userId) {
        String requestId = RequestContext.getRequestIdSafe();
        userFacade.disableUser(userId);
        return ResponseEntity.ok(ResponseEnvelope.successMessage(
                "User account disabled successfully", requestId
        ));
    }

    /**
     * GET /api/v1/users/{userId}/exists
     * Internal endpoint used by other services to validate user IDs.
     * Not exposed through API Gateway to public clients.
     */
    @GetMapping("/{userId}/exists")
    public ResponseEntity<ResponseEnvelope<Boolean>> userExists(@PathVariable String userId) {
        String requestId = RequestContext.getRequestIdSafe();
        // Direct service call - no facade needed for simple existence check
        // This is called by other microservices, not end users
        boolean exists = userService.userExistsAndEnabled(userId);
        return ResponseEntity.ok(ResponseEnvelope.success(
                exists, AppConstants.MSG_FETCHED, requestId
        ));
    }
}
