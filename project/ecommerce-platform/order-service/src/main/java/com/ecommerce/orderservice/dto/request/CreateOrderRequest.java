package com.ecommerce.orderservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateOrderRequest {

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemRequest> items;

    @NotBlank(message = "Shipping address is required")
    @Size(max = 500)
    private String shippingAddress;

    @Size(max = 500)
    private String notes;
}
