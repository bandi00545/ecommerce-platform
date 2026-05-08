package com.ecommerce.productservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdateProductRequest {

    @Size(min = 2, max = 255)
    private String name;

    private String description;

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal price;

    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @Size(max = 100)
    private String category;

    @Size(max = 500)
    private String imageUrl;

    @Size(max = 100)
    private String brand;

    @Min(0)
    private Integer weightGrams;

    private Boolean active;
}
