package com.ecommerce.productservice.controller;

import com.ecommerce.common.constants.AppConstants;
import com.ecommerce.common.context.RequestContext;
import com.ecommerce.common.dto.request.RequestEnvelope;
import com.ecommerce.common.dto.request.StockUpdateRequest;
import com.ecommerce.common.dto.response.PageResponse;
import com.ecommerce.common.dto.response.ProductSummaryDTO;
import com.ecommerce.common.dto.response.ResponseEnvelope;
import com.ecommerce.productservice.dto.request.CreateProductRequest;
import com.ecommerce.productservice.dto.request.UpdateProductRequest;
import com.ecommerce.productservice.dto.response.ProductResponse;
import com.ecommerce.productservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // =========================================================================
    // PUBLIC — Read endpoints (no authentication required)
    // =========================================================================

    /** GET /api/v1/products?page=0&size=10&sortBy=createdAt&sortDir=desc */
    @GetMapping
    public ResponseEntity<ResponseEnvelope<PageResponse<ProductResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0")         int    page,
            @RequestParam(defaultValue = "10")        int    size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDir) {

        String rid  = RequestContext.getRequestIdSafe();
        Sort   sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        return ResponseEntity.ok(ResponseEnvelope.success(
                productService.getAllProducts(
                        PageRequest.of(page, Math.min(size, 100), sort), rid),
                AppConstants.MSG_FETCHED, rid));
    }

    /** GET /api/v1/products/{productId} */
    @GetMapping("/{productId}")
    public ResponseEntity<ResponseEnvelope<ProductResponse>> getProductById(
            @PathVariable String productId) {

        String rid = RequestContext.getRequestIdSafe();
        return ResponseEntity.ok(ResponseEnvelope.success(
                productService.getProductById(productId, rid),
                AppConstants.MSG_FETCHED, rid));
    }

    /** GET /api/v1/products/sku/{sku} */
    @GetMapping("/sku/{sku}")
    public ResponseEntity<ResponseEnvelope<ProductResponse>> getProductBySku(
            @PathVariable String sku) {

        String rid = RequestContext.getRequestIdSafe();
        return ResponseEntity.ok(ResponseEnvelope.success(
                productService.getProductBySku(sku, rid),
                AppConstants.MSG_FETCHED, rid));
    }

    /** GET /api/v1/products/search?q=term&page=0&size=10 */
    @GetMapping("/search")
    public ResponseEntity<ResponseEnvelope<PageResponse<ProductResponse>>> searchProducts(
            @RequestParam("q")                     String term,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        String rid = RequestContext.getRequestIdSafe();
        return ResponseEntity.ok(ResponseEnvelope.success(
                productService.searchProducts(term,
                        PageRequest.of(page, size, Sort.by("name")), rid),
                AppConstants.MSG_FETCHED, rid));
    }

    /** GET /api/v1/products/category/{category}?page=0&size=10 */
    @GetMapping("/category/{category}")
    public ResponseEntity<ResponseEnvelope<PageResponse<ProductResponse>>> getByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        String rid = RequestContext.getRequestIdSafe();
        return ResponseEntity.ok(ResponseEnvelope.success(
                productService.getProductsByCategory(category,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()), rid),
                AppConstants.MSG_FETCHED, rid));
    }

    /** GET /api/v1/products/price-range?minPrice=100&maxPrice=5000&page=0&size=10 */
    @GetMapping("/price-range")
    public ResponseEntity<ResponseEnvelope<PageResponse<ProductResponse>>> getByPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        String rid = RequestContext.getRequestIdSafe();
        return ResponseEntity.ok(ResponseEnvelope.success(
                productService.getProductsByPriceRange(minPrice, maxPrice,
                        PageRequest.of(page, size, Sort.by("price")), rid),
                AppConstants.MSG_FETCHED, rid));
    }

    // =========================================================================
    // ADMIN — Write endpoints (ADMIN role enforced by API Gateway)
    // =========================================================================

    /** POST /api/v1/products */
    @PostMapping
    public ResponseEntity<ResponseEnvelope<ProductResponse>> createProduct(
            @Valid @RequestBody RequestEnvelope<CreateProductRequest> envelope) {

        String rid = RequestContext.getRequestIdSafe();
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseEnvelope.success(
                productService.createProduct(envelope.getPayload(), rid),
                AppConstants.MSG_CREATED, rid));
    }

    /** PUT /api/v1/products/{productId} */
    @PutMapping("/{productId}")
    public ResponseEntity<ResponseEnvelope<ProductResponse>> updateProduct(
            @PathVariable String productId,
            @Valid @RequestBody RequestEnvelope<UpdateProductRequest> envelope) {

        String rid = RequestContext.getRequestIdSafe();
        return ResponseEntity.ok(ResponseEnvelope.success(
                productService.updateProduct(productId, envelope.getPayload(), rid),
                AppConstants.MSG_UPDATED, rid));
    }

    /** DELETE /api/v1/products/{productId} — soft delete */
    @DeleteMapping("/{productId}")
    public ResponseEntity<ResponseEnvelope<Void>> deleteProduct(
            @PathVariable String productId) {

        String rid = RequestContext.getRequestIdSafe();
        productService.deleteProduct(productId, rid);
        return ResponseEntity.ok(ResponseEnvelope.successMessage(
                AppConstants.MSG_DELETED, rid));
    }

    // =========================================================================
    // INTERNAL — called by Order Service Saga (NOT exposed via API Gateway)
    // These endpoints are reachable only within the Docker/K8s network
    // =========================================================================

    @GetMapping("/internal/{productId}")
    public ResponseEntity<ResponseEnvelope<ProductSummaryDTO>> getProductSummary(
            @PathVariable String productId) {

        String rid = RequestContext.getRequestIdSafe();
        return ResponseEntity.ok(ResponseEnvelope.success(
                productService.getProductSummaryById(productId, rid),
                AppConstants.MSG_FETCHED, rid));
    }

    /**
     * POST /api/v1/products/internal/stock/reduce
     *
     * Atomically reduces stock. Called during order creation (Saga step 1).
     * Uses StockUpdateRequest from ecommerce-common (shared with order-service).
     */
    @PostMapping("/internal/stock/reduce")
    public ResponseEntity<ResponseEnvelope<Void>> reduceStock(
            @Valid @RequestBody StockUpdateRequest request) {

        String rid = RequestContext.getRequestIdSafe();
        productService.reduceStock(request, rid);
        return ResponseEntity.ok(ResponseEnvelope.successMessage(
                "Stock reduced successfully", rid));
    }

    /**
     * POST /api/v1/products/internal/stock/restore
     *
     * Atomically restores stock. Called during Saga compensation.
     */
    @PostMapping("/internal/stock/restore")
    public ResponseEntity<ResponseEnvelope<Void>> restoreStock(
            @Valid @RequestBody StockUpdateRequest request) {

        String rid = RequestContext.getRequestIdSafe();
        productService.restoreStock(request, rid);
        return ResponseEntity.ok(ResponseEnvelope.successMessage(
                "Stock restored successfully", rid));
    }
}
