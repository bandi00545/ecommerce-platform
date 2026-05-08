package com.ecommerce.orderservice.service;

import com.ecommerce.common.dto.response.PageResponse;
import com.ecommerce.common.enums.OrderStatus;
import com.ecommerce.orderservice.dto.request.CreateOrderRequest;
import com.ecommerce.orderservice.dto.response.OrderResponse;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request, String userId, String requestId);
    OrderResponse getOrderById(String orderId, String userId, String requestId);
    PageResponse<OrderResponse> getMyOrders(String userId, Pageable pageable, String requestId);
    PageResponse<OrderResponse> getAllOrders(Pageable pageable, String requestId);
    PageResponse<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable, String requestId);
    OrderResponse cancelOrder(String orderId, String userId, String requestId);
}
