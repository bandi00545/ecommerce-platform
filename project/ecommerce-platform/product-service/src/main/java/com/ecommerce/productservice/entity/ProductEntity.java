package com.ecommerce.productservice.entity;

import com.ecommerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(
    name = "products",
    indexes = {
        @Index(name = "idx_products_sku",      columnList = "sku",       unique = true),
        @Index(name = "idx_products_category", columnList = "category"),
        @Index(name = "idx_products_active",   columnList = "active"),
        @Index(name = "idx_products_price",    columnList = "price")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProductEntity extends BaseEntity {

    /** Human-readable product name displayed in UI */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Stock Keeping Unit - unique alphanumeric identifier.
     * Used for inventory management and external system integration.
     * Format: CATEGORY-XXXXX e.g. ELEC-00001, BOOK-00042
     */
    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    /** Full product description for product detail page */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Price in INR (Indian Rupee) stored as BigDecimal.
     * Scale: 2 decimal places (paise precision).
     * NEVER use float/double for monetary values.
     * precision=12 supports up to ₹9,999,999,999.99
     */
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /**
     * Available stock quantity.
     * Reduced by Order Service when order is confirmed.
     * Restored by Order Service during Saga compensation.
     * NEVER goes below 0 (enforced by DB CHECK constraint).
     */
    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    /** Product category for filtering and navigation */
    @Column(name = "category", nullable = false, length = 100)
    private String category;

    /** Primary product image URL (stored in object storage) */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * Soft delete flag.
     * false = product removed from catalog (but history preserved in orders).
     * Never physically delete products.
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /** Brand name for filtering and display */
    @Column(name = "brand", length = 100)
    private String brand;

    /**
     * Product weight in grams.
     * Used for shipping cost calculation.
     */
    @Column(name = "weight_grams")
    private Integer weightGrams;

    /**
     * Average customer rating (1.0 to 5.0).
     * Updated by a separate review service (future).
     * Stored here for search/sort efficiency.
     */
    @Column(name = "average_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    /** Total number of reviews (for displaying "X reviews" in UI) */
    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;
}
