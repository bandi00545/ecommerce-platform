package com.ecommerce.auditservice.controller;

import com.ecommerce.auditservice.entity.AuditLogEntity;
import com.ecommerce.auditservice.repository.AuditLogRepository;
import com.ecommerce.common.constants.AppConstants;
import com.ecommerce.common.context.RequestContext;
import com.ecommerce.common.dto.response.PageResponse;
import com.ecommerce.common.dto.response.ResponseEnvelope;
import com.ecommerce.common.enums.AuditStatus;
import com.ecommerce.common.enums.ServiceName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping("/trace/{requestId}")
    public ResponseEntity<ResponseEnvelope<List<AuditLogEntity>>> traceRequest(
            @PathVariable String requestId) {

        String rid = RequestContext.getRequestIdSafe();
        List<AuditLogEntity> logs =
                auditLogRepository.findAllByRequestIdOrderByEventTimestampAsc(requestId);

        log.info("Audit trace | requestId={} | found={} events", requestId, logs.size());
        return ResponseEntity.ok(ResponseEnvelope.success(
                logs, "Trace fetched for requestId: " + requestId, rid));
    }

    /**
     * GET /api/v1/audit/user/{userId}?page=0&size=20
     *
     * All audit events for a specific user — compliance / GDPR queries.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ResponseEnvelope<PageResponse<AuditLogEntity>>> getUserAuditLogs(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        String rid = RequestContext.getRequestIdSafe();
        Page<AuditLogEntity> result = auditLogRepository
                .findAllByUserIdOrderByEventTimestampDesc(
                        userId, PageRequest.of(page, Math.min(size, 100)));

        return ResponseEntity.ok(ResponseEnvelope.success(
                PageResponse.from(result, e -> e), AppConstants.MSG_FETCHED, rid));
    }

    /**
     * GET /api/v1/audit/entity/{entityType}/{entityId}
     *
     * All events for a specific entity — e.g. all events for Order #88.
     * EXAMPLE: /api/v1/audit/entity/Order/88
     */
    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<ResponseEnvelope<List<AuditLogEntity>>> getEntityAuditLogs(
            @PathVariable String entityType,
            @PathVariable String entityId) {

        String rid = RequestContext.getRequestIdSafe();
        List<AuditLogEntity> logs =
                auditLogRepository.findAllByEntityTypeAndEntityIdOrderByEventTimestampAsc(
                        entityType, entityId);

        return ResponseEntity.ok(ResponseEnvelope.success(
                logs, "Audit trail for " + entityType + " #" + entityId, rid));
    }

    /**
     * GET /api/v1/audit/failures?since=2024-01-15T00:00:00
     *
     * Recent failures across all services — for the ops / SRE dashboard.
     */
    @GetMapping("/failures")
    public ResponseEntity<ResponseEnvelope<List<AuditLogEntity>>> getRecentFailures(
            @RequestParam(defaultValue = "2024-01-01T00:00:00")
            @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime since) {

        String rid = RequestContext.getRequestIdSafe();
        List<AuditLogEntity> failures = auditLogRepository.findRecentFailures(since);

        log.info("Recent failures query | since={} | found={}", since, failures.size());
        return ResponseEntity.ok(ResponseEnvelope.success(
                failures, "Recent failures fetched", rid));
    }

    /**
     * GET /api/v1/audit/service/{serviceName}?page=0&size=20
     *
     * All events from a specific service.
     * EXAMPLE: /api/v1/audit/service/PAYMENT_SERVICE
     */
    @GetMapping("/service/{serviceName}")
    public ResponseEntity<ResponseEnvelope<PageResponse<AuditLogEntity>>> getByService(
            @PathVariable ServiceName serviceName,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        String rid = RequestContext.getRequestIdSafe();
        Page<AuditLogEntity> result = auditLogRepository
                .findAllByServiceNameOrderByEventTimestampDesc(
                        serviceName,
                        PageRequest.of(page, Math.min(size, 100),
                                Sort.by("eventTimestamp").descending()));

        return ResponseEntity.ok(ResponseEnvelope.success(
                PageResponse.from(result, e -> e), AppConstants.MSG_FETCHED, rid));
    }

    /**
     * GET /api/v1/audit/search?q=orderId+88&page=0&size=20
     *
     * Full-text search across description and error message fields.
     */
    @GetMapping("/search")
    public ResponseEntity<ResponseEnvelope<PageResponse<AuditLogEntity>>> searchLogs(
            @RequestParam("q") String term,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        String rid = RequestContext.getRequestIdSafe();
        Page<AuditLogEntity> result = auditLogRepository.searchLogs(
                term, PageRequest.of(page, Math.min(size, 100)));

        return ResponseEntity.ok(ResponseEnvelope.success(
                PageResponse.from(result, e -> e), AppConstants.MSG_FETCHED, rid));
    }

    /**
     * GET /api/v1/audit/range?from=...&to=...&serviceName=...&status=...&userId=...
     *
     * Time-range query with optional filters.
     * Used for compliance reports and executive dashboards.
     */
    @GetMapping("/range")
    public ResponseEntity<ResponseEnvelope<PageResponse<AuditLogEntity>>> getByRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime from,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime to,
            @RequestParam(required = false) ServiceName serviceName,
            @RequestParam(required = false) AuditStatus status,
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {

        String rid = RequestContext.getRequestIdSafe();
        Page<AuditLogEntity> result = auditLogRepository.findByTimeRange(
                from, to, serviceName, status, userId,
                PageRequest.of(page, Math.min(size, 100)));

        return ResponseEntity.ok(ResponseEnvelope.success(
                PageResponse.from(result, e -> e), AppConstants.MSG_FETCHED, rid));
    }

    /**
     * GET /api/v1/audit/health
     */
    @GetMapping("/health")
    public ResponseEntity<ResponseEnvelope<String>> health() {
        return ResponseEntity.ok(ResponseEnvelope.success(
                "Audit Service is running", RequestContext.getRequestIdSafe()));
    }
}
