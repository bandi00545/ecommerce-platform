package com.ecommerce.productservice.service;

import com.ecommerce.common.dto.request.StockUpdateRequest;
import com.ecommerce.common.dto.response.PageResponse;
import com.ecommerce.common.dto.response.ProductSummaryDTO;
import com.ecommerce.productservice.dto.request.CreateProductRequest;
import com.ecommerce.productservice.dto.request.UpdateProductRequest;
import com.ecommerce.productservice.dto.response.ProductResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ProductService {

    ProductResponse    createProduct(CreateProductRequest request, String requestId);
    ProductResponse    getProductById(String productId, String requestId);
    ProductSummaryDTO  getProductSummaryById(String productId, String requestId);
    ProductResponse    getProductBySku(String sku, String requestId);
    PageResponse<ProductResponse> getAllProducts(Pageable pageable, String requestId);
    PageResponse<ProductResponse> searchProducts(String term, Pageable pageable, String requestId);
    PageResponse<ProductResponse> getProductsByCategory(String category, Pageable pageable, String requestId);
    PageResponse<ProductResponse> getProductsByPriceRange(BigDecimal min, BigDecimal max, Pageable pageable, String requestId);
    ProductResponse    updateProduct(String productId, UpdateProductRequest request, String requestId);
    void               deleteProduct(String productId, String requestId);
    void               reduceStock(StockUpdateRequest request, String requestId);
    void               restoreStock(StockUpdateRequest request, String requestId);
}
