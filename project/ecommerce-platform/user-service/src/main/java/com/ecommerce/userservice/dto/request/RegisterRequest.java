package com.ecommerce.userservice.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    @Pattern(
        regexp = "^[A-Za-z0-9_]+$",
        message = "Username can only contain letters, numbers, and underscores"
    )
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character (@#$%^&+=!)"
    )
    private String password;

    @Pattern(
        regexp = "^[6-9]\\d{9}$",
        message = "Phone must be a valid 10-digit Indian mobile number starting with 6-9"
    )
    private String phone;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;
}
