package com.ecommerce.orderservice.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderItemResponse {
    private String id;
    private String productId;
    private String productName;
    private String productSku;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}
