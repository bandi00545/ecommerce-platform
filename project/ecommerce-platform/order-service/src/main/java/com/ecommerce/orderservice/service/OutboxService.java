package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.entity.OutboxEntity;
import com.ecommerce.orderservice.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // Maximum retry attempts before logging as stuck
    private static final int MAX_RETRY_BEFORE_ALERT = 5;

    // =========================================================================
    // SAVE TO OUTBOX (called within business transaction)
    // =========================================================================

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveToOutbox(String aggregateType, String aggregateId, String topic,
                              String eventType, String messageKey, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);

            OutboxEntity outboxEvent = OutboxEntity.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .topic(topic)
                    .eventType(eventType)
                    .messageKey(messageKey)
                    .payload(payloadJson)
                    .processed(false)
                    .createdAt(LocalDateTime.now())
                    .retryCount(0)
                    .build();

            outboxRepository.save(outboxEvent);
            log.debug("Saved outbox event | type={} | aggregateId={} | topic={}",
                    eventType, aggregateId, topic);

        } catch (Exception e) {
            log.error("Failed to serialize outbox payload | type={} | error={}",
                    eventType, e.getMessage(), e);
            throw new RuntimeException("Failed to save outbox event: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // OUTBOX POLLER (runs every 5 seconds)
    // =========================================================================

    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    @Transactional
    public void processOutbox() {
        List<OutboxEntity> pending = outboxRepository.findUnprocessedEvents();

        if (pending.isEmpty()) return;

        log.debug("Outbox poller: processing {} pending events", pending.size());

        for (OutboxEntity event : pending) {
            try {
                // Publish to Kafka
                kafkaTemplate.send(event.getTopic(), event.getMessageKey(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Outbox publish failed | id={} | topic={} | error={}",
                                        event.getId(), event.getTopic(), ex.getMessage());
                                outboxRepository.incrementRetryCount(event.getId(), ex.getMessage());
                            }
                        });

                // Mark as processed immediately (optimistic: Kafka is usually reliable)
                outboxRepository.markAsProcessed(event.getId(), LocalDateTime.now());

                log.debug("Outbox event published | id={} | type={} | topic={}",
                        event.getId(), event.getEventType(), event.getTopic());

            } catch (Exception e) {
                log.error("Outbox processing error | id={} | error={}", event.getId(), e.getMessage());
                outboxRepository.incrementRetryCount(event.getId(), e.getMessage());

                // Alert if stuck (high retry count)
                if (event.getRetryCount() >= MAX_RETRY_BEFORE_ALERT) {
                    log.error("ALERT: Outbox event stuck after {} retries | id={} | type={}",
                            event.getRetryCount(), event.getId(), event.getEventType());
                }
            }
        }
    }
}
