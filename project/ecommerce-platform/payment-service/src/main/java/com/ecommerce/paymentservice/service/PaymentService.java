package com.ecommerce.paymentservice.service;

import com.ecommerce.common.constants.AppConstants;
import com.ecommerce.common.dto.audit.AuditLogDTO;
import com.ecommerce.common.dto.event.PaymentRequestEvent;
import com.ecommerce.common.dto.event.PaymentResponseEvent;
import com.ecommerce.common.enums.AuditStatus;
import com.ecommerce.common.enums.PaymentStatus;
import com.ecommerce.common.enums.ServiceName;
import com.ecommerce.paymentservice.entity.PaymentEntity;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository             paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper                  objectMapper;

    @Value("${app.kafka.topic.payment-response:payment.response}")
    private String paymentResponseTopic;

    @Value("${app.kafka.topic.audit:audit.events}")
    private String auditTopic;

    @Value("${app.payment.failure-rate:0.1}")
    private double failureRate;

    @Value("${app.payment.processing-delay-ms:500}")
    private long processingDelayMs;

    private final Random random = new Random();

    // =========================================================================
    // KAFKA LISTENER: payment.request
    // =========================================================================

    @KafkaListener(
            topics           = "${app.kafka.topic.payment-request:payment.request}",
            groupId          = "${spring.kafka.consumer.group-id:payment-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void processPaymentRequest(String message) {
        PaymentRequestEvent event;
        try {
            event = objectMapper.readValue(message, PaymentRequestEvent.class);
        } catch (Exception e) {
            log.error("Cannot deserialize PaymentRequestEvent: {}", e.getMessage());
            return;
        }

        log.info("Processing payment | orderId={} | amount={} | requestId={}",
                event.getOrderId(), event.getAmount(), event.getRequestId());

        // IDEMPOTENCY: re-publish existing result if already processed
        if (paymentRepository.existsByOrderId(event.getOrderId())) {
            log.warn("Duplicate payment request — re-publishing result | orderId={}",
                    event.getOrderId());
            paymentRepository.findByOrderId(event.getOrderId())
                    .ifPresent(existing -> publishPaymentResponse(existing));
            return;
        }

        // Create initial record
        PaymentEntity payment = PaymentEntity.builder()
                .orderId(event.getOrderId())
                .requestId(event.getRequestId())
                .userId(event.getUserId())
                .amount(event.getAmount())
                .status(PaymentStatus.PROCESSING)
                .build();
        paymentRepository.save(payment);

        // Simulate gateway processing delay
        try {
            Thread.sleep(processingDelayMs);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        // Simulate gateway call — replace with real SDK (Razorpay, Stripe, PayU)
        if (simulateGatewayCall()) {
            String gwRef = "GW-" + UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 8).toUpperCase();
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setGatewayReference(gwRef);
            log.info("Payment SUCCESS | orderId={} | gwRef={}", event.getOrderId(), gwRef);
            publishAudit(event.getRequestId(), event.getUserId(), AuditStatus.SUCCESS,
                    "Payment succeeded for orderId=" + event.getOrderId(), event.getOrderId(), null);
        } else {
            String reason = pickFailureReason();
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(reason);
            log.warn("Payment FAILED | orderId={} | reason={}", event.getOrderId(), reason);
            publishAudit(event.getRequestId(), event.getUserId(), AuditStatus.FAILURE,
                    "Payment failed: " + reason, event.getOrderId(), reason);
        }

        PaymentEntity saved = paymentRepository.save(payment);
        publishPaymentResponse(saved);
    }

    // =========================================================================
    // KAFKA LISTENER: payment.refund.request (Saga compensation)
    // =========================================================================

    @KafkaListener(
            topics           = "payment.refund.request",
            groupId          = "${spring.kafka.consumer.group-id:payment-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void processRefundRequest(String message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> refundData = objectMapper.readValue(message, Map.class);
            String orderId   = (String) refundData.get("orderId");
            String paymentId = (String) refundData.get("paymentId");
            String requestId = (String) refundData.get("requestId");
            String userId    = (String) refundData.get("userId");

            log.info("Processing refund | orderId={} | paymentId={}", orderId, paymentId);

            paymentRepository.findById(paymentId).ifPresent(payment -> {
                if (!payment.isRefunded()
                        && payment.getStatus() == PaymentStatus.SUCCESS) {
                    payment.setStatus(PaymentStatus.REFUNDED);
                    payment.setRefunded(true);
                    paymentRepository.save(payment);
                    log.info("Refund completed | paymentId={} | orderId={}", paymentId, orderId);
                    publishAudit(requestId, userId, AuditStatus.SUCCESS,
                            "Refund completed for orderId=" + orderId, paymentId, null);
                }
            });
        } catch (Exception e) {
            log.error("Refund processing failed: {}", e.getMessage(), e);
        }
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private void publishPaymentResponse(PaymentEntity payment) {
        PaymentResponseEvent response = PaymentResponseEvent.builder()
                .orderId(payment.getOrderId())
                .requestId(payment.getRequestId())
                .paymentId(payment.getId())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .failureReason(payment.getFailureReason())
                .processedAt(LocalDateTime.now())
                .build();

        try {
            kafkaTemplate.send(paymentResponseTopic, payment.getOrderId(), response);
            log.debug("Payment response published | orderId={} | status={}",
                    payment.getOrderId(), payment.getStatus());
        } catch (Exception e) {
            log.error("CRITICAL: Failed to publish payment response | orderId={} | error={}",
                    payment.getOrderId(), e.getMessage());
        }
    }

    private void publishAudit(String requestId, String userId, AuditStatus status,
                               String description, String entityId, String error) {
        try {
            AuditLogDTO audit = AuditLogDTO.builder()
                    .requestId(requestId)
                    .userId(userId)
                    .serviceName(ServiceName.PAYMENT_SERVICE)
                    .action(AppConstants.ACTION_PAYMENT)
                    .status(status)
                    .description(description)
                    .entityType("Payment")
                    .entityId(entityId)
                    .errorMessage(error)
                    .timestamp(LocalDateTime.now())
                    .build();
            kafkaTemplate.send(auditTopic, requestId, audit);
        } catch (Exception e) {
            log.error("Audit publish failed in PaymentService: {}", e.getMessage());
        }
    }

    /** Simulates gateway decision. In production: replace with real SDK call. */
    private boolean simulateGatewayCall() {
        return random.nextDouble() > failureRate;
    }

    private String pickFailureReason() {
        String[] reasons = {
            "Insufficient funds",
            "Card expired",
            "Transaction declined by issuing bank",
            "Daily limit exceeded",
            "Invalid card details"
        };
        return reasons[random.nextInt(reasons.length)];
    }
}
