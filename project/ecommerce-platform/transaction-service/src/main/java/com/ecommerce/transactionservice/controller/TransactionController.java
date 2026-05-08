package com.ecommerce.transactionservice.controller;

import com.ecommerce.common.constants.AppConstants;
import com.ecommerce.common.context.RequestContext;
import com.ecommerce.common.dto.response.ResponseEnvelope;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.enums.ErrorCode;
import com.ecommerce.transactionservice.entity.TransactionEntity;
import com.ecommerce.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepository transactionRepository;

    /** GET /api/v1/transactions/health */
    @GetMapping("/health")
    public ResponseEntity<ResponseEnvelope<String>> health() {
        return ResponseEntity.ok(ResponseEnvelope.success(
                "Transaction Service is running",
                RequestContext.getRequestIdSafe()
        ));
    }

    /**
     * GET /api/v1/transactions/{transactionId}
     * Fetch a transaction record by its ID.
     * Used by: admin support, reconciliation jobs.
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<ResponseEnvelope<TransactionEntity>> getById(
            @PathVariable String transactionId) {

        String requestId = RequestContext.getRequestIdSafe();
        TransactionEntity txn = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TRANSACTION_NOT_FOUND,
                        "Transaction not found with id: " + transactionId
                ));

        return ResponseEntity.ok(ResponseEnvelope.success(txn, AppConstants.MSG_FETCHED, requestId));
    }

    /**
     * GET /api/v1/transactions/order/{orderId}
     * Fetch a transaction by orderId.
     * Used by: Order Service queries, customer support.
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ResponseEnvelope<TransactionEntity>> getByOrderId(
            @PathVariable String orderId) {

        String requestId = RequestContext.getRequestIdSafe();
        TransactionEntity txn = transactionRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TRANSACTION_NOT_FOUND,
                        "No transaction found for orderId: " + orderId
                ));

        return ResponseEntity.ok(ResponseEnvelope.success(txn, AppConstants.MSG_FETCHED, requestId));
    }

    /**
     * GET /api/v1/transactions/payment/{paymentId}
     * Fetch a transaction by paymentId.
     */
    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<ResponseEnvelope<TransactionEntity>> getByPaymentId(
            @PathVariable String paymentId) {

        String requestId = RequestContext.getRequestIdSafe();
        TransactionEntity txn = transactionRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TRANSACTION_NOT_FOUND,
                        "No transaction found for paymentId: " + paymentId
                ));

        return ResponseEntity.ok(ResponseEnvelope.success(txn, AppConstants.MSG_FETCHED, requestId));
    }
}
