package com.ecommerce.common.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductSummaryDTO {
    private String id;
    private String name;
    private String sku;
    private BigDecimal price;
    private Integer stockQuantity;
    private boolean active;
    private boolean inStock;
}
