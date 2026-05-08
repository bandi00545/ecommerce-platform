package com.ecommerce.productservice.service;

import com.ecommerce.common.constants.AppConstants;
import com.ecommerce.common.dto.audit.AuditLogDTO;
import com.ecommerce.common.dto.request.StockUpdateRequest;
import com.ecommerce.common.dto.response.PageResponse;
import com.ecommerce.common.dto.response.ProductSummaryDTO;
import com.ecommerce.common.enums.AuditStatus;
import com.ecommerce.common.enums.ErrorCode;
import com.ecommerce.common.enums.ServiceName;
import com.ecommerce.common.exception.DuplicateResourceException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.exception.ValidationException;
import com.ecommerce.productservice.dto.request.CreateProductRequest;
import com.ecommerce.productservice.dto.request.UpdateProductRequest;
import com.ecommerce.productservice.dto.response.ProductResponse;
import com.ecommerce.productservice.entity.ProductEntity;
import com.ecommerce.productservice.mapper.ProductMapper;
import com.ecommerce.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository              productRepository;
    private final ProductMapper                  productMapper;
    private final KafkaTemplate<String, AuditLogDTO> kafkaTemplate;

    @Value("${app.kafka.topic.audit:audit.events}")
    private String auditTopic;

    // =========================================================================
    // CREATE
    // =========================================================================

    @Override
    @Transactional
    @CacheEvict(value = "productPages", allEntries = true)
    public ProductResponse createProduct(CreateProductRequest request, String requestId) {
        log.info("Creating product | requestId={} | sku={}", requestId, request.getSku());

        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException(ErrorCode.PRODUCT_ALREADY_EXISTS,
                    "Product with SKU '" + request.getSku() + "' already exists.");
        }

        ProductEntity entity = productMapper.toEntity(request);
        entity.setActive(true);
        ProductEntity saved = productRepository.save(entity);

        publishAudit(requestId, null, AppConstants.ACTION_CREATE, AuditStatus.SUCCESS,
                "Product created: " + saved.getSku(), "Product", saved.getId(), null);

        log.info("Product created | id={} | sku={}", saved.getId(), saved.getSku());
        return productMapper.toResponse(saved);
    }

    // =========================================================================
    // GET BY ID (cached) — full ProductResponse
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'product:' + #productId", unless = "#result == null")
    public ProductResponse getProductById(String productId, String requestId) {
        ProductEntity entity = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.PRODUCT_NOT_FOUND,
                        "Product not found with id: " + productId));
        return productMapper.toResponse(entity);
    }

    // =========================================================================
    // GET SUMMARY BY ID — lightweight DTO for order-service internal use
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public ProductSummaryDTO getProductSummaryById(String productId, String requestId) {
        ProductEntity entity = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.PRODUCT_NOT_FOUND,
                        "Product not found with id: " + productId));
        return toSummaryDTO(entity);
    }

    // =========================================================================
    // GET BY SKU (cached)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'sku:' + #sku", unless = "#result == null")
    public ProductResponse getProductBySku(String sku, String requestId) {
        ProductEntity entity = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.PRODUCT_NOT_FOUND,
                        "Product not found with SKU: " + sku));
        return productMapper.toResponse(entity);
    }

    // =========================================================================
    // GET ALL (paginated, cached)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "productPages",
               key = "'page:' + #pageable.pageNumber + ':' + #pageable.pageSize",
               unless = "#result == null")
    public PageResponse<ProductResponse> getAllProducts(Pageable pageable, String requestId) {
        Page<ProductEntity> page = productRepository.findAllByActiveTrue(pageable);
        return PageResponse.from(page, productMapper::toResponse);
    }

    // =========================================================================
    // SEARCH
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(String term, Pageable pageable,
                                                         String requestId) {
        Page<ProductEntity> page = productRepository.searchActiveProducts(term, pageable);
        return PageResponse.from(page, productMapper::toResponse);
    }

    // =========================================================================
    // GET BY CATEGORY
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProductsByCategory(String category,
                                                                Pageable pageable,
                                                                String requestId) {
        Page<ProductEntity> page =
                productRepository.findAllByCategoryAndActiveTrue(category, pageable);
        return PageResponse.from(page, productMapper::toResponse);
    }

    // =========================================================================
    // GET BY PRICE RANGE
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProductsByPriceRange(BigDecimal minPrice,
                                                                   BigDecimal maxPrice,
                                                                   Pageable pageable,
                                                                   String requestId) {
        if (minPrice.compareTo(maxPrice) > 0) {
            throw new ValidationException("minPrice cannot be greater than maxPrice");
        }
        Page<ProductEntity> page =
                productRepository.findByPriceRange(minPrice, maxPrice, pageable);
        return PageResponse.from(page, productMapper::toResponse);
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products",     key = "'product:' + #productId"),
        @CacheEvict(value = "productPages", allEntries = true)
    })
    public ProductResponse updateProduct(String productId, UpdateProductRequest request,
                                          String requestId) {
        ProductEntity entity = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.PRODUCT_NOT_FOUND,
                        "Product not found with id: " + productId));

        productMapper.updateEntityFromRequest(request, entity);
        ProductEntity updated = productRepository.save(entity);

        publishAudit(requestId, null, AppConstants.ACTION_UPDATE, AuditStatus.SUCCESS,
                "Product updated: " + productId, "Product", productId, null);

        return productMapper.toResponse(updated);
    }

    // =========================================================================
    // SOFT DELETE
    // =========================================================================

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products",     key = "'product:' + #productId"),
        @CacheEvict(value = "productPages", allEntries = true)
    })
    public void deleteProduct(String productId, String requestId) {
        ProductEntity entity = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.PRODUCT_NOT_FOUND,
                        "Product not found with id: " + productId));

        entity.setActive(false);
        productRepository.save(entity);

        publishAudit(requestId, null, AppConstants.ACTION_DELETE, AuditStatus.SUCCESS,
                "Product soft-deleted: " + productId, "Product", productId, null);
    }

    // =========================================================================
    // REDUCE STOCK (Saga step — called by Order Service)
    // StockUpdateRequest is from ecommerce-common (shared)
    // =========================================================================

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "'product:' + #request.productId")
    public void reduceStock(StockUpdateRequest request, String requestId) {
        log.info("Reducing stock | productId={} | qty={} | orderId={} | requestId={}",
                request.getProductId(), request.getQuantity(),
                request.getOrderId(), requestId);

        // Atomic decrement — fails if stock would go negative
        int rows = productRepository.decrementStock(
                request.getProductId(), request.getQuantity());

        if (rows == 0) {
            ProductEntity product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ErrorCode.PRODUCT_NOT_FOUND,
                            "Product not found: " + request.getProductId()));

            throw new ValidationException(ErrorCode.INSUFFICIENT_STOCK,
                    "Insufficient stock for '" + product.getSku()
                            + "'. Available: " + product.getStockQuantity()
                            + ", Requested: " + request.getQuantity());
        }

        publishAudit(requestId, null, AppConstants.ACTION_STOCK_REDUCE, AuditStatus.SUCCESS,
                "Stock reduced by " + request.getQuantity()
                        + " for product " + request.getProductId()
                        + " (orderId=" + request.getOrderId() + ")",
                "Product", request.getProductId(), null);
    }

    // =========================================================================
    // RESTORE STOCK (Saga compensation — never throws hard)
    // =========================================================================

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "'product:' + #request.productId")
    public void restoreStock(StockUpdateRequest request, String requestId) {
        log.info("Restoring stock (compensation) | productId={} | qty={} | orderId={}",
                request.getProductId(), request.getQuantity(), request.getOrderId());

        int rows = productRepository.incrementStock(
                request.getProductId(), request.getQuantity());

        if (rows == 0) {
            log.error("COMPENSATION FAIL: product not found for stock restore | productId={}",
                    request.getProductId());
            publishAudit(requestId, null, AppConstants.ACTION_STOCK_RESTORE, AuditStatus.FAILURE,
                    "COMPENSATION FAILED: could not restore stock for product "
                            + request.getProductId(),
                    "Product", request.getProductId(), "PRODUCT_NOT_FOUND");
            return; // Do NOT throw — compensation must always complete
        }

        publishAudit(requestId, null, AppConstants.ACTION_STOCK_RESTORE, AuditStatus.SUCCESS,
                "Stock restored by " + request.getQuantity()
                        + " for product " + request.getProductId()
                        + " (orderId=" + request.getOrderId() + ")",
                "Product", request.getProductId(), null);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Maps ProductEntity → ProductSummaryDTO (lightweight view for order-service).
     * Kept here (not in mapper) to avoid adding a dependency on common in the mapper interface.
     */
    private ProductSummaryDTO toSummaryDTO(ProductEntity entity) {
        return ProductSummaryDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .sku(entity.getSku())
                .price(entity.getPrice())
                .stockQuantity(entity.getStockQuantity())
                .active(entity.isActive())
                .inStock(entity.getStockQuantity() != null && entity.getStockQuantity() > 0)
                .build();
    }

    /** Publishes audit event asynchronously — non-blocking, never fails silently. */
    @Async("taskExecutor")
    public void publishAudit(String requestId, String userId, String action,
                                  AuditStatus status, String description,
                                  String entityType, String entityId, String errorMsg) {
        try {
            AuditLogDTO audit = AuditLogDTO.builder()
                    .requestId(requestId)
                    .userId(userId)
                    .serviceName(ServiceName.PRODUCT_SERVICE)
                    .action(action)
                    .status(status)
                    .description(description)
                    .entityType(entityType)
                    .entityId(entityId)
                    .errorMessage(errorMsg)
                    .timestamp(LocalDateTime.now())
                    .build();
            kafkaTemplate.send(auditTopic, requestId, audit);
        } catch (Exception e) {
            log.error("Audit publish failed | action={} | error={}", action, e.getMessage());
        }
    }
}
