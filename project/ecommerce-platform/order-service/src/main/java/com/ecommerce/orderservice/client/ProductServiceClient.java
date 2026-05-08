package com.ecommerce.orderservice.client;

import com.ecommerce.common.constants.AppConstants;
import com.ecommerce.common.dto.request.StockUpdateRequest;
import com.ecommerce.common.dto.response.ProductSummaryDTO;
import com.ecommerce.common.dto.response.ResponseEnvelope;
import com.ecommerce.common.enums.ErrorCode;
import com.ecommerce.common.exception.ServiceUnavailableException;
import com.ecommerce.common.exception.ValidationException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductServiceClient {

    private final RestTemplate loadBalancedRestTemplate;

    @Value("${app.services.product-url:http://product-service}")
    private String productServiceUrl;

    // =========================================================================
    // GET PRODUCT BY ID
    // =========================================================================

    @CircuitBreaker(name = AppConstants.CB_PRODUCT_SERVICE, fallbackMethod = "getProductFallback")
    @Retry(name = AppConstants.CB_PRODUCT_SERVICE)
    public ProductSummaryDTO getProductById(String productId, String requestId) {
        log.debug("GET product-service /api/v1/products/{} | requestId={}", productId, requestId);

        ResponseEntity<ResponseEnvelope<ProductSummaryDTO>> response =
                loadBalancedRestTemplate.exchange(
                        productServiceUrl + "/api/v1/products/internal/" + productId,
                        HttpMethod.GET,
                        buildEntity(requestId, null),
                        new ParameterizedTypeReference<ResponseEnvelope<ProductSummaryDTO>>() {}
                );

        ResponseEnvelope<ProductSummaryDTO> body = response.getBody();
        if (body == null || body.isFailure() || body.getData() == null) {
            String msg = (body != null) ? body.getMessage() : "No response body";
            throw new ServiceUnavailableException(ErrorCode.SERVICE_UNAVAILABLE,
                    "Product service failed for product " + productId + ": " + msg);
        }
        return body.getData();
    }

    // =========================================================================
    // REDUCE STOCK (called within order creation transaction)
    // =========================================================================

    @CircuitBreaker(name = AppConstants.CB_PRODUCT_SERVICE, fallbackMethod = "stockReduceFallback")
    @Retry(name = AppConstants.CB_PRODUCT_SERVICE)
    public void reduceStock(String productId, int quantity, String orderId, String requestId) {
        log.info("Reducing stock | productId={} | qty={} | orderId={} | requestId={}",
                productId, quantity, orderId, requestId);

        StockUpdateRequest req = StockUpdateRequest.builder()
                .productId(productId).quantity(quantity)
                .orderId(orderId).requestId(requestId)
                .build();

        ResponseEntity<ResponseEnvelope<Void>> response =
                loadBalancedRestTemplate.exchange(
                        productServiceUrl + "/api/v1/products/internal/stock/reduce",
                        HttpMethod.POST,
                        buildEntity(requestId, req),
                        new ParameterizedTypeReference<ResponseEnvelope<Void>>() {}
                );

        ResponseEnvelope<Void> body = response.getBody();
        if (body != null && body.isFailure()) {
            throw new ValidationException(
                    body.getMessage() != null ? body.getMessage()
                            : "Stock reduction failed for product: " + productId);
        }
    }

    // =========================================================================
    // RESTORE STOCK (Saga compensation — never throws)
    // =========================================================================

    @CircuitBreaker(name = AppConstants.CB_PRODUCT_SERVICE, fallbackMethod = "stockRestoreFallback")
    public void restoreStock(String productId, int quantity, String orderId, String requestId) {
        log.info("Restoring stock | productId={} | qty={} | orderId={}", productId, quantity, orderId);

        StockUpdateRequest req = StockUpdateRequest.builder()
                .productId(productId).quantity(quantity)
                .orderId(orderId).requestId(requestId)
                .build();

        loadBalancedRestTemplate.exchange(
                productServiceUrl + "/api/v1/products/internal/stock/restore",
                HttpMethod.POST,
                buildEntity(requestId, req),
                new ParameterizedTypeReference<ResponseEnvelope<Void>>() {}
        );
    }

    // =========================================================================
    // FALLBACK METHODS
    // =========================================================================

    public ProductSummaryDTO getProductFallback(String productId, String requestId, Throwable t) {
        log.error("Product CB open | productId={} | cause={}", productId, t.getMessage());
        throw new ServiceUnavailableException(ErrorCode.CIRCUIT_BREAKER_OPEN,
                "Product service unavailable. Try again later.");
    }

    public void stockReduceFallback(String productId, int qty,
                                     String orderId, String requestId, Throwable t) {
        log.error("Stock reduce CB open | productId={} | cause={}", productId, t.getMessage());
        throw new ServiceUnavailableException(ErrorCode.CIRCUIT_BREAKER_OPEN,
                "Product service unavailable — cannot reserve stock.");
    }

    /** Compensation MUST NOT throw — log and swallow silently */
    public void stockRestoreFallback(String productId, int qty,
                                      String orderId, String requestId, Throwable t) {
        log.error("COMPENSATION WARN: stock restore CB open | productId={} | cause={}",
                productId, t.getMessage());
    }

    // =========================================================================
    // HELPER
    // =========================================================================

    private <T> HttpEntity<T> buildEntity(String requestId, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(AppConstants.HEADER_REQUEST_ID, requestId);
        return new HttpEntity<>(body, headers);
    }
}
