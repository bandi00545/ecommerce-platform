package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.entity.OutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEntity, String> {

    /**
     * Fetch unprocessed events ordered by creation time.
     * OutboxPoller calls this on every scheduled run.
     * Limit 50: process in small batches to avoid memory pressure.
     */
    @Query("""
            SELECT o FROM OutboxEntity o
            WHERE o.processed = false
            ORDER BY o.createdAt ASC
            """)
    List<OutboxEntity> findUnprocessedEvents();

    /**
     * Mark an event as processed after successful Kafka publish.
     */
    @Modifying
    @Query("""
            UPDATE OutboxEntity o
            SET o.processed = true, o.processedAt = :processedAt
            WHERE o.id = :id
            """)
    void markAsProcessed(@Param("id") String id,
                          @Param("processedAt") LocalDateTime processedAt);

    /**
     * Increment retry counter and record error on failed publish.
     */
    @Modifying
    @Query("""
            UPDATE OutboxEntity o
            SET o.retryCount = o.retryCount + 1, o.lastError = :error
            WHERE o.id = :id
            """)
    void incrementRetryCount(@Param("id") String id, @Param("error") String error);

    /** Find stuck events (high retry count) for alerting */
    List<OutboxEntity> findByProcessedFalseAndRetryCountGreaterThan(int retryCount);
}
