package com.ecommerce.auditservice.consumer;

import com.ecommerce.auditservice.entity.AuditLogEntity;
import com.ecommerce.auditservice.repository.AuditLogRepository;
import com.ecommerce.common.dto.audit.AuditLogDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditKafkaConsumer {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper       objectMapper;

    @KafkaListener(
            topics    = "${app.kafka.topic.audit:audit.events}",
            groupId   = "${spring.kafka.consumer.group-id:audit-service-group}",
            containerFactory = "batchKafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeAuditEvents(List<ConsumerRecord<String, String>> records) {
        if (records.isEmpty()) return;

        log.debug("Audit consumer: processing batch of {} events", records.size());

        List<AuditLogEntity> entities = new ArrayList<>(records.size());

        for (ConsumerRecord<String, String> record : records) {
            try {
                AuditLogDTO dto = objectMapper.readValue(record.value(), AuditLogDTO.class);
                AuditLogEntity entity = mapToEntity(dto, record);
                entities.add(entity);
            } catch (Exception e) {
                // Log malformed message and continue — don't fail the whole batch
                log.error("Failed to deserialize audit event | topic={} | partition={} | offset={} | error={}",
                        record.topic(), record.partition(), record.offset(), e.getMessage());
            }
        }

        if (!entities.isEmpty()) {
            // Single saveAll → one DB batch INSERT → high throughput
            auditLogRepository.saveAll(entities);
            log.debug("Audit batch persisted: {} records saved", entities.size());
        }
    }

    // =========================================================================
    // PRIVATE: Map DTO → Entity
    // =========================================================================

    private AuditLogEntity mapToEntity(AuditLogDTO dto, ConsumerRecord<String, String> record) {
        return AuditLogEntity.builder()
                .requestId(dto.getRequestId())
                .userId(dto.getUserId())
                .serviceName(dto.getServiceName())
                .action(dto.getAction())
                .status(dto.getStatus())
                .description(dto.getDescription())
                .errorMessage(dto.getErrorMessage())
                .ipAddress(dto.getIpAddress())
                .userAgent(dto.getUserAgent())
                .entityType(dto.getEntityType())
                .entityId(dto.getEntityId())
                .eventTimestamp(dto.getTimestamp() != null
                        ? dto.getTimestamp()
                        : LocalDateTime.now())
                .insertedAt(LocalDateTime.now())
                .build();
    }
}
