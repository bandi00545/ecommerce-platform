package com.ecommerce.orderservice.service;

import com.ecommerce.common.dto.response.PageResponse;
import com.ecommerce.common.dto.response.ProductSummaryDTO;
import com.ecommerce.common.enums.ErrorCode;
import com.ecommerce.common.enums.OrderStatus;
import com.ecommerce.common.exception.ForbiddenException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.exception.ValidationException;
import com.ecommerce.orderservice.client.ProductServiceClient;
import com.ecommerce.orderservice.dto.request.CreateOrderRequest;
import com.ecommerce.orderservice.dto.request.OrderItemRequest;
import com.ecommerce.orderservice.dto.response.OrderResponse;
import com.ecommerce.orderservice.entity.OrderEntity;
import com.ecommerce.orderservice.entity.OrderItemEntity;
import com.ecommerce.orderservice.mapper.OrderMapper;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.saga.OrderSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository       orderRepository;
    private final OrderSagaOrchestrator sagaOrchestrator;
    private final ProductServiceClient  productServiceClient;
    private final OrderMapper           orderMapper;

    // =========================================================================
    // CREATE ORDER
    // =========================================================================

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String userId, String requestId) {
        log.info("Creating order | requestId={} | userId={} | itemCount={}",
                requestId, userId, request.getItems().size());

        // IDEMPOTENCY: return existing order if requestId already processed
        Optional<OrderEntity> existing = orderRepository.findByRequestId(requestId);
        if (existing.isPresent()) {
            log.info("Idempotent: returning existing order | requestId={} | orderId={}",
                    requestId, existing.get().getId());
            return orderMapper.toResponse(existing.get());
        }
        return doCreateOrder(request, userId, requestId);
    }

    private OrderResponse doCreateOrder(CreateOrderRequest request, String userId, String requestId) {
        List<OrderItemEntity> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        // ---------------------------------------------------------------
        // Validate each product and reserve stock
        // ---------------------------------------------------------------
        for (OrderItemRequest itemReq : request.getItems()) {

            // Fetch product summary (uses ProductSummaryDTO from common — no cross-module dep)
            ProductSummaryDTO product = productServiceClient.getProductById(
                    itemReq.getProductId(), requestId);

            // Business validation
            if (!product.isActive()) {
                throw new ValidationException(
                        "Product '" + product.getName() + "' is no longer available.");
            }
            if (!product.isInStock()
                    || product.getStockQuantity() < itemReq.getQuantity()) {
                throw new ValidationException(
                        ErrorCode.INSUFFICIENT_STOCK,
                        "Insufficient stock for '" + product.getName()
                                + "'. Available: " + product.getStockQuantity()
                                + ", Requested: " + itemReq.getQuantity());
            }

            // Reserve stock (sync REST call inside this transaction)
            productServiceClient.reduceStock(
                    itemReq.getProductId(),
                    itemReq.getQuantity(),
                    null,   // orderId not yet assigned
                    requestId);

            // Build order item with price snapshot (immutable after creation)
            OrderItemEntity item = OrderItemEntity.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(product.getPrice())   // PRICE SNAPSHOT
                    .build();

            orderItems.add(item);
            total = total.add(
                    product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));
        }

        // ---------------------------------------------------------------
        // Build and persist the order
        // ---------------------------------------------------------------
        OrderEntity order = OrderEntity.builder()
                .userId(userId)
                .requestId(requestId)
                .status(OrderStatus.CONFIRMED)   // stock has been reserved
                .shippingAddress(request.getShippingAddress())
                .notes(request.getNotes())
                .totalAmount(total)
                .build();

        for (OrderItemEntity item : orderItems) {
            order.addItem(item);
        }

        OrderEntity saved = orderRepository.save(order);

        // ---------------------------------------------------------------
        // Start Saga — saves PaymentRequestEvent to outbox in SAME TX
        // ---------------------------------------------------------------
        sagaOrchestrator.startSaga(saved);

        log.info("Order created and saga started | orderId={} | total={}", saved.getId(), total);
        return orderMapper.toResponse(saved);
    }

    // =========================================================================
    // GET ORDER BY ID
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(String orderId, String userId, String requestId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ORDER_NOT_FOUND, "Order not found: " + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new ForbiddenException(
                    ErrorCode.ORDER_BELONGS_TO_ANOTHER_USER,
                    "You do not have permission to view this order.");
        }
        return orderMapper.toResponse(order);
    }

    // =========================================================================
    // GET MY ORDERS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(String userId, Pageable pageable, String requestId) {
        Page<OrderEntity> page =
                orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.from(page, orderMapper::toResponse);
    }

    // =========================================================================
    // GET ALL ORDERS (Admin)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(Pageable pageable, String requestId) {
        Page<OrderEntity> page = orderRepository.findAllByOrderByCreatedAtDesc(pageable);
        return PageResponse.from(page, orderMapper::toResponse);
    }

    // =========================================================================
    // GET ORDERS BY STATUS (Admin)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrdersByStatus(OrderStatus status,
                                                          Pageable pageable,
                                                          String requestId) {
        Page<OrderEntity> page =
                orderRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable);
        return PageResponse.from(page, orderMapper::toResponse);
    }

    // =========================================================================
    // CANCEL ORDER
    // =========================================================================

    @Override
    @Transactional
    public OrderResponse cancelOrder(String orderId, String userId, String requestId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ORDER_NOT_FOUND, "Order not found: " + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new ForbiddenException("You cannot cancel another user's order.");
        }

        if (order.getStatus() != OrderStatus.PENDING
                && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new ValidationException(ErrorCode.ORDER_CANNOT_BE_CANCELLED,
                    "Cannot cancel order in status: " + order.getStatus()
                            + ". Only PENDING or CONFIRMED orders can be cancelled.");
        }

        // Release reserved stock for all items
        for (OrderItemEntity item : order.getItems()) {
            try {
                productServiceClient.restoreStock(
                        item.getProductId(), item.getQuantity(), orderId, requestId);
            } catch (Exception e) {
                log.error("Stock restore failed on cancel | productId={} | error={}",
                        item.getProductId(), e.getMessage());
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setFailureReason("Cancelled by user");
        OrderEntity saved = orderRepository.save(order);

        log.info("Order cancelled | orderId={} | userId={}", orderId, userId);
        return orderMapper.toResponse(saved);
    }
}
