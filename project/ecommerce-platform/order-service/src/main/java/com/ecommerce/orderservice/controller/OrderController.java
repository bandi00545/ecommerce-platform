package com.ecommerce.orderservice.controller;

import com.ecommerce.common.constants.AppConstants;
import com.ecommerce.common.context.RequestContext;
import com.ecommerce.common.dto.request.RequestEnvelope;
import com.ecommerce.common.dto.response.PageResponse;
import com.ecommerce.common.dto.response.ResponseEnvelope;
import com.ecommerce.common.enums.OrderStatus;
import com.ecommerce.orderservice.dto.request.CreateOrderRequest;
import com.ecommerce.orderservice.dto.response.OrderResponse;
import com.ecommerce.orderservice.service.OrderService;
import com.ecommerce.orderservice.service.SagaLogService;
import com.ecommerce.orderservice.entity.SagaLogEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService    orderService;
    private final SagaLogService  sagaLogService;

    /** POST /api/v1/orders - Create a new order (starts saga) */
    @PostMapping
    public ResponseEntity<ResponseEnvelope<OrderResponse>> createOrder(
            @Valid @RequestBody RequestEnvelope<CreateOrderRequest> envelope) {

        String requestId = RequestContext.getRequestIdSafe();
        String userId    = RequestContext.getUserId();

        OrderResponse order = orderService.createOrder(
                envelope.getPayload(), userId, requestId);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ResponseEnvelope.success(order,
                        "Order submitted successfully. Processing payment...", requestId));
    }

    /** GET /api/v1/orders/my - Current user's orders */
    @GetMapping("/my")
    public ResponseEntity<ResponseEnvelope<PageResponse<OrderResponse>>> getMyOrders(
            @RequestParam(defaultValue = "0")         int page,
            @RequestParam(defaultValue = "10")        int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {

        String requestId = RequestContext.getRequestIdSafe();
        String userId    = RequestContext.getUserId();

        return ResponseEntity.ok(ResponseEnvelope.success(
                orderService.getMyOrders(userId,
                        PageRequest.of(page, size, Sort.by(sortBy).descending()), requestId),
                AppConstants.MSG_FETCHED, requestId));
    }

    /** GET /api/v1/orders/{orderId} - Get specific order */
    @GetMapping("/{orderId}")
    public ResponseEntity<ResponseEnvelope<OrderResponse>> getOrder(
            @PathVariable String orderId) {

        String requestId = RequestContext.getRequestIdSafe();
        String userId    = RequestContext.getUserId();

        return ResponseEntity.ok(ResponseEnvelope.success(
                orderService.getOrderById(orderId, userId, requestId),
                AppConstants.MSG_FETCHED, requestId));
    }

    /** DELETE /api/v1/orders/{orderId} - Cancel an order */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<ResponseEnvelope<OrderResponse>> cancelOrder(
            @PathVariable String orderId) {

        String requestId = RequestContext.getRequestIdSafe();
        String userId    = RequestContext.getUserId();

        return ResponseEntity.ok(ResponseEnvelope.success(
                orderService.cancelOrder(orderId, userId, requestId),
                "Order cancelled successfully", requestId));
    }

    /** GET /api/v1/orders - Admin: all orders */
    @GetMapping
    public ResponseEntity<ResponseEnvelope<PageResponse<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0")         int page,
            @RequestParam(defaultValue = "10")        int size,
            @RequestParam(required = false)           OrderStatus status) {

        String requestId = RequestContext.getRequestIdSafe();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by("createdAt").descending());

        PageResponse<OrderResponse> result = status != null
                ? orderService.getOrdersByStatus(status, pageable, requestId)
                : orderService.getAllOrders(pageable, requestId);

        return ResponseEntity.ok(ResponseEnvelope.success(result, AppConstants.MSG_FETCHED, requestId));
    }

    /** GET /api/v1/orders/{orderId}/saga-logs - Admin: saga execution trace */
    @GetMapping("/{orderId}/saga-logs")
    public ResponseEntity<ResponseEnvelope<List<SagaLogEntity>>> getSagaLogs(
            @PathVariable String orderId) {

        String requestId = RequestContext.getRequestIdSafe();
        return ResponseEntity.ok(ResponseEnvelope.success(
                sagaLogService.getSagaLogs(orderId),
                "Saga logs fetched", requestId));
    }
}
