package com.ecommerce.paymentservice.repository;

import com.ecommerce.paymentservice.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, String> {
    Optional<PaymentEntity> findByOrderId(String orderId);
    Optional<PaymentEntity> findByRequestId(String requestId);
    boolean existsByOrderId(String orderId);
}
