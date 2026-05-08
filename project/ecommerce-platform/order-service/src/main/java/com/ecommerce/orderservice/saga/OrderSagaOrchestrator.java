package com.ecommerce.orderservice.saga;

import com.ecommerce.common.constants.AppConstants;
import com.ecommerce.common.dto.event.PaymentRequestEvent;
import com.ecommerce.common.dto.event.PaymentResponseEvent;
import com.ecommerce.common.dto.event.TransactionRequestEvent;
import com.ecommerce.common.dto.event.TransactionResponseEvent;
import com.ecommerce.common.enums.AuditStatus;
import com.ecommerce.common.enums.OrderStatus;
import com.ecommerce.common.enums.PaymentStatus;
import com.ecommerce.common.enums.TransactionStatus;
import com.ecommerce.orderservice.client.ProductServiceClient;
import com.ecommerce.orderservice.entity.OrderEntity;
import com.ecommerce.orderservice.entity.OrderItemEntity;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.service.OutboxService;
import com.ecommerce.orderservice.service.SagaLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSagaOrchestrator {

    private final OrderRepository      orderRepository;
    private final OutboxService        outboxService;
    private final SagaLogService       sagaLogService;
    private final ProductServiceClient productServiceClient;
    private final ObjectMapper         objectMapper;

    @Value("${app.kafka.topic.payment-request:payment.request}")
    private String paymentRequestTopic;

    @Value("${app.kafka.topic.transaction-request:transaction.request}")
    private String transactionRequestTopic;

    // =========================================================================
    // STEP 1: START SAGA — called inside OrderServiceImpl.createOrder() TX
    // =========================================================================

    @Transactional
    public void startSaga(OrderEntity order) {
        log.info("Starting Saga | orderId={} | requestId={} | total={}",
                order.getId(), order.getRequestId(), order.getTotalAmount());

        sagaLogService.logStep(
                order.getId(), order.getRequestId(),
                AppConstants.SAGA_STEP_ORDER_CREATED, AuditStatus.SUCCESS,
                "Order created with " + order.getItems().size()
                        + " items, total=" + order.getTotalAmount(),
                null);

        PaymentRequestEvent paymentRequest = PaymentRequestEvent.builder()
                .orderId(order.getId())
                .requestId(order.getRequestId())
                .userId(order.getUserId())
                .amount(order.getTotalAmount())
                .requestedAt(LocalDateTime.now())
                .build();

        sagaLogService.logStep(
                order.getId(), order.getRequestId(),
                AppConstants.SAGA_STEP_PAYMENT_INIT, AuditStatus.PARTIAL,
                "Payment request queued for amount=" + order.getTotalAmount(),
                null);

        // Save to outbox — same transaction as order save → atomic guarantee
        outboxService.saveToOutbox(
                "Order",
                order.getId(),
                paymentRequestTopic,
                "PAYMENT_REQUEST",
                order.getRequestId(),
                paymentRequest);

        log.info("Saga step 1 complete: payment request in outbox | orderId={}", order.getId());
    }

    // =========================================================================
    // STEP 2: HANDLE PAYMENT RESPONSE (Kafka Consumer)
    // =========================================================================

    /**
     * Listens for payment results published by Payment Service.
     * Message arrives as a raw JSON string (StringDeserializer).
     * We parse manually for precise error handling.
     */
    @KafkaListener(
            topics           = "${app.kafka.topic.payment-response:payment.response}",
            groupId          = "${spring.kafka.consumer.group-id:order-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onPaymentResponse(String message) {
        PaymentResponseEvent event;
        try {
            event = objectMapper.readValue(message, PaymentResponseEvent.class);
        } catch (Exception e) {
            log.error("Cannot deserialize PaymentResponseEvent: {}", e.getMessage());
            return;
        }

        log.info("Payment response | orderId={} | status={} | paymentId={}",
                event.getOrderId(), event.getStatus(), event.getPaymentId());

        OrderEntity order = orderRepository.findById(event.getOrderId()).orElse(null);
        if (order == null) {
            log.error("Payment response for unknown orderId={}", event.getOrderId());
            return;
        }

        // Idempotency guard — only process if order is in expected state
        if (order.getStatus() != OrderStatus.CONFIRMED
                && order.getStatus() != OrderStatus.PENDING) {
            log.warn("Ignoring payment response — order already in status={} | orderId={}",
                    order.getStatus(), order.getId());
            return;
        }

        if (event.getStatus() == PaymentStatus.SUCCESS) {
            handlePaymentSuccess(order, event);
        } else {
            handlePaymentFailure(order, event);
        }
    }

    // =========================================================================
    // STEP 3: HANDLE TRANSACTION RESPONSE (Kafka Consumer)
    // =========================================================================

    @KafkaListener(
            topics           = "${app.kafka.topic.transaction-response:transaction.response}",
            groupId          = "${spring.kafka.consumer.group-id:order-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onTransactionResponse(String message) {
        TransactionResponseEvent event;
        try {
            event = objectMapper.readValue(message, TransactionResponseEvent.class);
        } catch (Exception e) {
            log.error("Cannot deserialize TransactionResponseEvent: {}", e.getMessage());
            return;
        }

        log.info("Transaction response | orderId={} | status={} | txnId={}",
                event.getOrderId(), event.getStatus(), event.getTransactionId());

        OrderEntity order = orderRepository.findById(event.getOrderId()).orElse(null);
        if (order == null) {
            log.error("Transaction response for unknown orderId={}", event.getOrderId());
            return;
        }

        // Idempotency guard
        if (order.getStatus() != OrderStatus.PROCESSING) {
            log.warn("Ignoring transaction response — order in status={} | orderId={}",
                    order.getStatus(), order.getId());
            return;
        }

        if (event.getStatus() == TransactionStatus.COMPLETED) {
            handleTransactionSuccess(order, event);
        } else {
            handleTransactionFailure(order, event);
        }
    }

    // =========================================================================
    // PRIVATE — Payment Success Handler
    // =========================================================================

    private void handlePaymentSuccess(OrderEntity order, PaymentResponseEvent event) {
        log.info("Payment SUCCESS | orderId={} | paymentId={}", order.getId(), event.getPaymentId());

        orderRepository.updateStatusAndPaymentId(
                order.getId(), OrderStatus.PROCESSING, event.getPaymentId());

        sagaLogService.logStep(
                order.getId(), order.getRequestId(),
                AppConstants.SAGA_STEP_PAYMENT_SUCCESS, AuditStatus.SUCCESS,
                "Payment succeeded. paymentId=" + event.getPaymentId(),
                null);

        TransactionRequestEvent txnRequest = TransactionRequestEvent.builder()
                .orderId(order.getId())
                .requestId(order.getRequestId())
                .paymentId(event.getPaymentId())
                .userId(order.getUserId())
                .amount(order.getTotalAmount())
                .requestedAt(LocalDateTime.now())
                .build();

        outboxService.saveToOutbox(
                "Order",
                order.getId(),
                transactionRequestTopic,
                "TRANSACTION_REQUEST",
                order.getRequestId(),
                txnRequest);

        sagaLogService.logStep(
                order.getId(), order.getRequestId(),
                AppConstants.SAGA_STEP_TXN_RECORDED, AuditStatus.PARTIAL,
                "Transaction request queued for paymentId=" + event.getPaymentId(),
                null);

        log.info("Saga step 2 complete: transaction request in outbox | orderId={}", order.getId());
    }

    // =========================================================================
    // PRIVATE — Payment Failure + Compensation
    // =========================================================================

    private void handlePaymentFailure(OrderEntity order, PaymentResponseEvent event) {
        log.warn("Payment FAILED | orderId={} | reason={}", order.getId(), event.getFailureReason());

        orderRepository.updateStatusAndFailureReason(
                order.getId(), OrderStatus.PAYMENT_FAILED, event.getFailureReason());

        sagaLogService.logStep(
                order.getId(), order.getRequestId(),
                AppConstants.SAGA_STEP_PAYMENT_FAILED, AuditStatus.FAILURE,
                "Payment failed: " + event.getFailureReason(),
                event.getFailureReason());

        sagaLogService.logStep(
                order.getId(), order.getRequestId(),
                AppConstants.SAGA_STEP_COMPENSATE_START, AuditStatus.PARTIAL,
                "Starting compensation: restoring stock for all items",
                null);

        compensateStock(order);

        orderRepository.updateStatusAndFailureReason(
                order.getId(), OrderStatus.COMPENSATED,
                "Payment failed: " + event.getFailureReason());

        sagaLogService.logStep(
                order.getId(), order.getRequestId(),
                AppConstants.SAGA_STEP_COMPENSATE_DONE, AuditStatus.SUCCESS,
                "Compensation complete: stock restored",
                null);

        log.info("Saga compensation done (payment failure) | orderId={}", order.getId());
    }

    // =========================================================================
    // PRIVATE — Transaction Success Handler
    // =========================================================================

    private void handleTransactionSuccess(OrderEntity order, TransactionResponseEvent event) {
        log.info("Transaction SUCCESS | orderId={} | txnId={}",
                order.getId(), event.getTransactionId());

        orderRepository.updateStatusAndTransactionId(
                order.getId(), OrderStatus.COMPLETED, event.getTransactionId());

        sagaLogService.logStep(
                order.getId(), order.getRequestId(),
                AppConstants.SAGA_STEP_TXN_RECORDED, AuditStatus.SUCCESS,
                "Transaction recorded. txnId=" + event.getTransactionId(),
                null);

        sagaLogService.logStep(
                order.getId(), order.getRequestId(),
                AppConstants.SAGA_STEP_ORDER_COMPLETE, AuditStatus.SUCCESS,
                "Order COMPLETED. total=" + order.getTotalAmount(),
                null);

        log.info("ORDER COMPLETED | orderId={} | total={}", order.getId(), order.getTotalAmount());
    }

    // =========================================================================
    // PRIVATE — Transaction Failure + Full Compensation
    // =========================================================================

    private void handleTransactionFailure(OrderEntity order, TransactionResponseEvent event) {
        log.error("Transaction FAILED | orderId={} | reason={}",
                order.getId(), event.getFailureReason());

        orderRepository.updateStatusAndFailureReason(
                order.getId(), OrderStatus.TRANSACTION_FAILED, event.getFailureReason());

        sagaLogService.logStep(
                order.getId(), order.getRequestId(),
                AppConstants.SAGA_STEP_TXN_FAILED, AuditStatus.FAILURE,
                "Transaction failed: " + event.getFailureReason(),
                event.getFailureReason());

        sagaLogService.logStep(
                order.getId(), order.getRequestId(),
                AppConstants.SAGA_STEP_COMPENSATE_START, AuditStatus.PARTIAL,
                "Starting full compensation: refund payment + restore stock",
                null);

        // Re-fetch to get latest paymentId
        OrderEntity fresh = orderRepository.findById(order.getId()).orElse(order);

        if (fresh.getPaymentId() != null) {
            publishRefundRequest(fresh, event.getFailureReason());
        }

        compensateStock(fresh);

        orderRepository.updateStatusAndFailureReason(
                fresh.getId(), OrderStatus.COMPENSATED,
                "Transaction failed: " + event.getFailureReason());

        sagaLogService.logStep(
                order.getId(), order.getRequestId(),
                AppConstants.SAGA_STEP_COMPENSATE_DONE, AuditStatus.SUCCESS,
                "Full compensation complete: payment refunded + stock restored",
                null);

        log.info("Saga full compensation done (txn failure) | orderId={}", order.getId());
    }

    // =========================================================================
    // PRIVATE — Restore Stock for ALL items (never throws)
    // =========================================================================

    private void compensateStock(OrderEntity order) {
        List<OrderItemEntity> items = order.getItems();
        for (OrderItemEntity item : items) {
            try {
                productServiceClient.restoreStock(
                        item.getProductId(),
                        item.getQuantity(),
                        order.getId(),
                        order.getRequestId());
                log.info("Stock restored | productId={} | qty={} | orderId={}",
                        item.getProductId(), item.getQuantity(), order.getId());
            } catch (Exception e) {
                // Log but continue — compensation must attempt all items
                log.error("COMPENSATION PARTIAL FAIL: stock restore | " +
                        "productId={} | qty={} | orderId={} | error={}",
                        item.getProductId(), item.getQuantity(), order.getId(),
                        e.getMessage());
            }
        }
    }

    // =========================================================================
    // PRIVATE — Publish Refund Request via Outbox
    // =========================================================================

    private void publishRefundRequest(OrderEntity order, String reason) {
        try {
            Map<String, Object> refundRequest = new HashMap<>();
            refundRequest.put("orderId",   order.getId());
            refundRequest.put("paymentId", order.getPaymentId());
            refundRequest.put("requestId", order.getRequestId());
            refundRequest.put("userId",    order.getUserId());
            refundRequest.put("amount",    order.getTotalAmount().toString());
            refundRequest.put("reason",    reason != null ? reason : "Transaction recording failed");

            outboxService.saveToOutbox(
                    "Order",
                    order.getId(),
                    "payment.refund.request",
                    "PAYMENT_REFUND_REQUEST",
                    order.getRequestId(),
                    refundRequest);

            log.info("Refund request queued | orderId={} | paymentId={}",
                    order.getId(), order.getPaymentId());
        } catch (Exception e) {
            log.error("CRITICAL: Failed to queue refund for paymentId={} | orderId={} | error={}",
                    order.getPaymentId(), order.getId(), e.getMessage());
            // In production: page on-call engineer for manual intervention
        }
    }
}
