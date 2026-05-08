package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.entity.SagaLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SagaLogRepository extends JpaRepository<SagaLogEntity, String> {
    /** All saga steps for a given order, chronological */
    List<SagaLogEntity> findAllByOrderIdOrderByCreatedAtAsc(String orderId);
}
