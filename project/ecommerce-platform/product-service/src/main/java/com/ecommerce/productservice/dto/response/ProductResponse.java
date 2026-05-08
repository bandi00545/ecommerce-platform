package com.ecommerce.productservice.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductResponse {
    private String id;
    private String name;
    private String sku;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String category;
    private String imageUrl;
    private boolean active;
    private String brand;
    private Integer weightGrams;
    private BigDecimal averageRating;
    private Integer reviewCount;
    private boolean inStock;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
