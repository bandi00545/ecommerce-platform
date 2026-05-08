package com.ecommerce.userservice.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @Pattern(
        regexp = "^[6-9]\\d{9}$",
        message = "Phone must be a valid 10-digit Indian mobile number"
    )
    private String phone;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @Size(max = 500, message = "Profile picture URL must not exceed 500 characters")
    private String profilePictureUrl;
}
