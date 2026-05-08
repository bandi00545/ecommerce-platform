package com.ecommerce.auditservice.repository;

import com.ecommerce.auditservice.entity.AuditLogEntity;
import com.ecommerce.common.enums.AuditStatus;
import com.ecommerce.common.enums.ServiceName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, String> {

    /** All events for a requestId — full trace across all services */
    List<AuditLogEntity> findAllByRequestIdOrderByEventTimestampAsc(String requestId);

    /** All events for a specific user — compliance queries */
    Page<AuditLogEntity> findAllByUserIdOrderByEventTimestampDesc(String userId, Pageable pageable);

    /** All events from a specific service */
    Page<AuditLogEntity> findAllByServiceNameOrderByEventTimestampDesc(
            ServiceName serviceName, Pageable pageable);

    /** All events of a specific action type */
    Page<AuditLogEntity> findAllByActionOrderByEventTimestampDesc(
            String action, Pageable pageable);

    /** All failures — for alerting dashboards */
    Page<AuditLogEntity> findAllByStatusOrderByEventTimestampDesc(
            AuditStatus status, Pageable pageable);

    /** All events for a specific entity (e.g. all events for orderId=88) */
    List<AuditLogEntity> findAllByEntityTypeAndEntityIdOrderByEventTimestampAsc(
            String entityType, String entityId);

    /**
     * Time-range query with optional filters.
     * Used for compliance reports and dashboards.
     */
    @Query("""
            SELECT a FROM AuditLogEntity a
            WHERE a.eventTimestamp BETWEEN :from AND :to
              AND (:serviceName IS NULL OR a.serviceName = :serviceName)
              AND (:status      IS NULL OR a.status      = :status)
              AND (:userId      IS NULL OR a.userId      = :userId)
            ORDER BY a.eventTimestamp DESC
            """)
    Page<AuditLogEntity> findByTimeRange(
            @Param("from")        LocalDateTime from,
            @Param("to")          LocalDateTime to,
            @Param("serviceName") ServiceName serviceName,
            @Param("status")      AuditStatus status,
            @Param("userId")      String userId,
            Pageable pageable);

    /**
     * Count failures by service in the last N minutes.
     * Used by monitoring/alerting to detect service degradation.
     */
    @Query("""
            SELECT COUNT(a) FROM AuditLogEntity a
            WHERE a.serviceName = :serviceName
              AND a.status = 'FAILURE'
              AND a.eventTimestamp >= :since
            """)
    long countFailuresSince(
            @Param("serviceName") ServiceName serviceName,
            @Param("since")       LocalDateTime since);

    /**
     * Recent FAILURE events across all services — for the ops dashboard.
     */
    @Query("""
            SELECT a FROM AuditLogEntity a
            WHERE a.status = 'FAILURE'
              AND a.eventTimestamp >= :since
            ORDER BY a.eventTimestamp DESC
            """)
    List<AuditLogEntity> findRecentFailures(@Param("since") LocalDateTime since);

    /**
     * Full-text search across description and errorMessage.
     * Supports "find all audit logs mentioning orderId X".
     */
    @Query("""
            SELECT a FROM AuditLogEntity a
            WHERE LOWER(a.description)  LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(a.errorMessage) LIKE LOWER(CONCAT('%', :term, '%'))
            ORDER BY a.eventTimestamp DESC
            """)
    Page<AuditLogEntity> searchLogs(@Param("term") String term, Pageable pageable);
}
