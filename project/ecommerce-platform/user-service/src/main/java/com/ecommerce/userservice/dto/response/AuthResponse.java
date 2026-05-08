package com.ecommerce.userservice.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /** JWT access token - include in Authorization: Bearer <token> header */
    private String accessToken;

    /** Token type - always "Bearer" */
    @Builder.Default
    private String tokenType = "Bearer";

    /** Number of minutes until the token expires */
    private long expiresInMinutes;

    /** Timestamp when the token expires */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;

    /** Basic user info for the client to display */
    private String userId;
    private String username;
    private String email;
    private String role;
    private String fullName;
}
