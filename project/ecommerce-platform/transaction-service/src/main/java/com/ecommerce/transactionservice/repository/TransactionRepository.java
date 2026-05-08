package com.ecommerce.transactionservice.repository;

import com.ecommerce.transactionservice.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {
    Optional<TransactionEntity> findByOrderId(String orderId);
    Optional<TransactionEntity> findByPaymentId(String paymentId);
    boolean existsByOrderId(String orderId);
}
