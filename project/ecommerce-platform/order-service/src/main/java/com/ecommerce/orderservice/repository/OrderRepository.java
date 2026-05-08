package com.ecommerce.orderservice.repository;

import com.ecommerce.common.enums.OrderStatus;
import com.ecommerce.orderservice.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, String> {

    /** Find by the idempotency key (requestId) - prevents duplicate order creation */
    Optional<OrderEntity> findByRequestId(String requestId);

    /** All orders for a specific user (customer's order history) */
    Page<OrderEntity> findAllByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /** All orders by status (admin dashboard) */
    Page<OrderEntity> findAllByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    /** All orders (admin) */
    Page<OrderEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Count orders by user and status (analytics) */
    long countByUserIdAndStatus(String userId, OrderStatus status);

    /**
     * Atomic status update. Used by Saga to avoid race conditions.
     * Only updates if current status matches expectedStatus (optimistic check).
     */
    @Modifying
    @Query("""
            UPDATE OrderEntity o SET o.status = :newStatus
            WHERE o.id = :orderId AND o.status = :expectedStatus
            """)
    int updateStatusConditional(
            @Param("orderId") String orderId,
            @Param("expectedStatus") OrderStatus expectedStatus,
            @Param("newStatus") OrderStatus newStatus);

    /**
     * Update status, paymentId in one shot (after payment success).
     */
    @Modifying
    @Query("""
            UPDATE OrderEntity o
            SET o.status = :status, o.paymentId = :paymentId
            WHERE o.id = :orderId
            """)
    void updateStatusAndPaymentId(
            @Param("orderId") String orderId,
            @Param("status") OrderStatus status,
            @Param("paymentId") String paymentId);

    /**
     * Update status, transactionId (after transaction success = COMPLETED).
     */
    @Modifying
    @Query("""
            UPDATE OrderEntity o
            SET o.status = :status, o.transactionId = :transactionId
            WHERE o.id = :orderId
            """)
    void updateStatusAndTransactionId(
            @Param("orderId") String orderId,
            @Param("status") OrderStatus status,
            @Param("transactionId") String transactionId);

    /**
     * Update status and failure reason.
     */
    @Modifying
    @Query("""
            UPDATE OrderEntity o
            SET o.status = :status, o.failureReason = :reason
            WHERE o.id = :orderId
            """)
    void updateStatusAndFailureReason(
            @Param("orderId") String orderId,
            @Param("status") OrderStatus status,
            @Param("reason") String reason);
}
