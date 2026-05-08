package com.ecommerce.transactionservice.service;

import com.ecommerce.common.constants.AppConstants;
import com.ecommerce.common.dto.audit.AuditLogDTO;
import com.ecommerce.common.dto.event.TransactionRequestEvent;
import com.ecommerce.common.dto.event.TransactionResponseEvent;
import com.ecommerce.common.enums.AuditStatus;
import com.ecommerce.common.enums.ServiceName;
import com.ecommerce.common.enums.TransactionStatus;
import com.ecommerce.transactionservice.entity.TransactionEntity;
import com.ecommerce.transactionservice.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository         transactionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper                  objectMapper;

    @Value("${app.kafka.topic.transaction-response:transaction.response}")
    private String transactionResponseTopic;

    @Value("${app.kafka.topic.audit:audit.events}")
    private String auditTopic;

    // =========================================================================
    // KAFKA LISTENER: transaction.request
    // =========================================================================

    @KafkaListener(
            topics           = "${app.kafka.topic.transaction-request:transaction.request}",
            groupId          = "${spring.kafka.consumer.group-id:transaction-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void processTransactionRequest(String message) {
        TransactionRequestEvent event;
        try {
            event = objectMapper.readValue(message, TransactionRequestEvent.class);
        } catch (Exception e) {
            log.error("Cannot deserialize TransactionRequestEvent: {}", e.getMessage());
            return;
        }

        log.info("Processing transaction | orderId={} | paymentId={} | amount={} | requestId={}",
                event.getOrderId(), event.getPaymentId(),
                event.getAmount(), event.getRequestId());

        // IDEMPOTENCY: re-publish existing result if already recorded
        if (transactionRepository.existsByOrderId(event.getOrderId())) {
            log.warn("Duplicate transaction request — re-publishing result | orderId={}",
                    event.getOrderId());
            transactionRepository.findByOrderId(event.getOrderId())
                    .ifPresent(existing -> publishTransactionResponse(existing, null));
            return;
        }

        // Create initial ledger entry
        TransactionEntity transaction = TransactionEntity.builder()
                .orderId(event.getOrderId())
                .paymentId(event.getPaymentId())
                .requestId(event.getRequestId())
                .userId(event.getUserId())
                .amount(event.getAmount())
                .status(TransactionStatus.INITIATED)
                .build();
        transactionRepository.save(transaction);

        // Simulate ledger write (replace with real accounting API / SAP / Oracle Financials)
        if (simulateLedgerWrite()) {
            String ledgerRef = "TXN-" + UUID.randomUUID()
                    .toString().replace("-", "").substring(0, 12).toUpperCase();

            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setLedgerReference(ledgerRef);
            TransactionEntity saved = transactionRepository.save(transaction);

            log.info("Transaction COMPLETED | orderId={} | txnId={} | ledgerRef={}",
                    event.getOrderId(), saved.getId(), ledgerRef);

            publishAudit(event.getRequestId(), event.getUserId(), AuditStatus.SUCCESS,
                    "Transaction recorded. ledgerRef=" + ledgerRef
                            + ", orderId=" + event.getOrderId(),
                    saved.getId(), null);

            publishTransactionResponse(saved, null);

        } else {
            String reason = "Ledger write failed — internal system error";
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(reason);
            TransactionEntity saved = transactionRepository.save(transaction);

            log.error("Transaction FAILED | orderId={} | txnId={} | reason={}",
                    event.getOrderId(), saved.getId(), reason);

            publishAudit(event.getRequestId(), event.getUserId(), AuditStatus.FAILURE,
                    "Transaction failed: " + reason + ", orderId=" + event.getOrderId(),
                    saved.getId(), reason);

            publishTransactionResponse(saved, reason);
        }
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private void publishTransactionResponse(TransactionEntity txn, String failureReason) {
        TransactionResponseEvent response = TransactionResponseEvent.builder()
                .orderId(txn.getOrderId())
                .requestId(txn.getRequestId())
                .transactionId(txn.getId())
                .status(txn.getStatus())
                .amount(txn.getAmount())
                .failureReason(txn.getFailureReason() != null
                        ? txn.getFailureReason() : failureReason)
                .processedAt(LocalDateTime.now())
                .build();

        try {
            kafkaTemplate.send(transactionResponseTopic, txn.getOrderId(), response);
            log.debug("Transaction response published | orderId={} | status={}",
                    txn.getOrderId(), txn.getStatus());
        } catch (Exception e) {
            log.error("CRITICAL: Failed to publish transaction response | orderId={} | error={}",
                    txn.getOrderId(), e.getMessage());
        }
    }

    private void publishAudit(String requestId, String userId, AuditStatus status,
                               String description, String entityId, String error) {
        try {
            AuditLogDTO audit = AuditLogDTO.builder()
                    .requestId(requestId)
                    .userId(userId)
                    .serviceName(ServiceName.TRANSACTION_SERVICE)
                    .action(AppConstants.ACTION_TRANSACTION)
                    .status(status)
                    .description(description)
                    .entityType("Transaction")
                    .entityId(entityId)
                    .errorMessage(error)
                    .timestamp(LocalDateTime.now())
                    .build();
            kafkaTemplate.send(auditTopic, requestId, audit);
        } catch (Exception e) {
            log.error("Audit publish failed in TransactionService: {}", e.getMessage());
        }
    }

    /**
     * Simulates ledger write.
     * In production: call real accounting/ledger API.
     * 98% success rate — ledger writes are very reliable.
     */
    private boolean simulateLedgerWrite() {
        return Math.random() > 0.02;
    }
}
