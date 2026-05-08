package com.ecommerce.orderservice.service;

import com.ecommerce.common.enums.AuditStatus;
import com.ecommerce.orderservice.entity.SagaLogEntity;
import com.ecommerce.orderservice.repository.SagaLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SagaLogService {

    private final SagaLogRepository sagaLogRepository;

    /**
     * Logs a saga step.
     * Propagation.REQUIRES_NEW: runs in its own transaction.
     * Even if the main saga transaction rolls back, the log entry is preserved.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logStep(String orderId, String requestId, String stepName,
                         AuditStatus status, String stepData, String errorMessage) {
        try {
            SagaLogEntity entry = SagaLogEntity.builder()
                    .orderId(orderId)
                    .requestId(requestId)
                    .stepName(stepName)
                    .status(status)
                    .stepData(stepData)
                    .errorMessage(errorMessage)
                    .createdAt(LocalDateTime.now())
                    .completedAt(status != AuditStatus.PARTIAL ? LocalDateTime.now() : null)
                    .build();

            sagaLogRepository.save(entry);
        } catch (Exception e) {
            // Never let logging failure break the saga
            log.error("Failed to log saga step | orderId={} | step={} | error={}",
                    orderId, stepName, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<SagaLogEntity> getSagaLogs(String orderId) {
        return sagaLogRepository.findAllByOrderIdOrderByCreatedAtAsc(orderId);
    }
}
