package com.ecommerce.productservice.repository;

import com.ecommerce.productservice.entity.ProductEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, String> {

    Optional<ProductEntity> findBySku(String sku);
    boolean existsBySku(String sku);

    /** Paginated active products */
    Page<ProductEntity> findAllByActiveTrue(Pageable pageable);

    /** Filter by category */
    Page<ProductEntity> findAllByCategoryAndActiveTrue(String category, Pageable pageable);

    /**
     * Full-text search across name, description, brand, category.
     */
    @Query("""
            SELECT p FROM ProductEntity p
            WHERE p.active = true
              AND (LOWER(p.name)        LIKE LOWER(CONCAT('%',:term,'%'))
               OR  LOWER(p.description) LIKE LOWER(CONCAT('%',:term,'%'))
               OR  LOWER(p.brand)       LIKE LOWER(CONCAT('%',:term,'%'))
               OR  LOWER(p.category)    LIKE LOWER(CONCAT('%',:term,'%')))
            """)
    Page<ProductEntity> searchActiveProducts(@Param("term") String term, Pageable pageable);

    /** Price range filter */
    @Query("""
            SELECT p FROM ProductEntity p
            WHERE p.active = true
              AND p.price BETWEEN :minPrice AND :maxPrice
            """)
    Page<ProductEntity> findByPriceRange(@Param("minPrice") BigDecimal minPrice,
                                          @Param("maxPrice") BigDecimal maxPrice,
                                          Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductEntity p WHERE p.id = :id")
    Optional<ProductEntity> findByIdWithLock(@Param("id") String id);

    /**
     * Atomic stock decrement - safer than read-modify-write pattern.
     * Checks constraint inline to prevent negative stock atomically.
     */
    @Modifying
    @Query("""
            UPDATE ProductEntity p
            SET p.stockQuantity = p.stockQuantity - :quantity
            WHERE p.id = :productId
              AND p.stockQuantity >= :quantity
            """)
    int decrementStock(@Param("productId") String productId,
                        @Param("quantity") int quantity);

    /**
     * Atomic stock increment (used during Saga compensation to restore stock).
     */
    @Modifying
    @Query("""
            UPDATE ProductEntity p
            SET p.stockQuantity = p.stockQuantity + :quantity
            WHERE p.id = :productId
            """)
    int incrementStock(@Param("productId") String productId,
                        @Param("quantity") int quantity);
}
