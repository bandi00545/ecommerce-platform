package com.ecommerce.userservice.service;

import com.ecommerce.common.dto.audit.AuditLogDTO;
import com.ecommerce.common.enums.AuditStatus;
import com.ecommerce.common.enums.ServiceName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditEventPublisher {

    private final KafkaTemplate<String, AuditLogDTO> kafkaTemplate;

    @Value("${app.kafka.topic.audit:audit.events}")
    private String auditTopic;

    @Async
    public void publishAuditEvent(
            String requestId,
            String userId,
            String action,
            AuditStatus status,
            String description,
            String entityType,
            String entityId,
            String errorMsg) {

        AuditLogDTO auditLog = AuditLogDTO.builder()
                .requestId(requestId)
                .userId(userId)
                .serviceName(ServiceName.USER_SERVICE)
                .action(action)
                .status(status)
                .description(description)
                .entityType(entityType)
                .entityId(entityId)
                .errorMessage(errorMsg)
                .timestamp(LocalDateTime.now())
                .build();

        publishAuditEvent(auditLog);
    }

    /**
     * Publishes a pre-built AuditLogDTO to Kafka.
     * Use this when you need full control over the audit log fields.
     *
     * @param auditLog the fully built audit log DTO
     */
    @Async
    public void publishAuditEvent(AuditLogDTO auditLog) {
        try {
            // requestId is used as Kafka message key for ordering guarantees
            // (all audit events from the same request go to the same partition)
            CompletableFuture<SendResult<String, AuditLogDTO>> future =
                    kafkaTemplate.send(auditTopic, auditLog.getRequestId(), auditLog);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish audit event | requestId={} | action={} | error={}",
                            auditLog.getRequestId(), auditLog.getAction(), ex.getMessage());
                } else {
                    log.debug("Audit event published | requestId={} | action={} | offset={}",
                            auditLog.getRequestId(), auditLog.getAction(),
                            result.getRecordMetadata().offset());
                }
            });

        } catch (Exception e) {
            // CRITICAL: Never let audit logging failure propagate to the caller
            log.error("Unexpected error publishing audit event | requestId={} | error={}",
                    auditLog.getRequestId(), e.getMessage(), e);
        }
    }
}
