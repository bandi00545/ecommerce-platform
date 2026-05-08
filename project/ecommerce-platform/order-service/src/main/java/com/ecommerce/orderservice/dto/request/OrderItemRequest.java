package com.ecommerce.orderservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderItemRequest {

    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 100, message = "Cannot order more than 100 units of a single item")
    private Integer quantity;
}
