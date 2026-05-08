package com.ecommerce.productservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    private String name;

    @NotBlank(message = "SKU is required")
    @Size(min = 3, max = 50, message = "SKU must be between 3 and 50 characters")
    @Pattern(regexp = "^[A-Z0-9\\-]+$", message = "SKU must contain only uppercase letters, digits, and hyphens")
    private String sku;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Price can have at most 10 integer digits and 2 decimal places")
    private BigDecimal price;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @NotBlank(message = "Category is required")
    @Size(max = 100)
    private String category;

    @Size(max = 500)
    private String imageUrl;

    @Size(max = 100)
    private String brand;

    @Min(value = 0, message = "Weight cannot be negative")
    private Integer weightGrams;
}
