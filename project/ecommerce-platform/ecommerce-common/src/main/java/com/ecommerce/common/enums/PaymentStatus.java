package com.ecommerce.common.enums;

public enum PaymentStatus {

    /**
     * Payment request created and sent to Payment Service.
     * No actual charge has occurred yet.
     */
    INITIATED,

    /**
     * Payment is actively being processed by the payment gateway.
     * Intermediate state during external gateway call.
     */
    PROCESSING,

    /**
     * Payment was successfully charged.
     * Transaction Service will now record the financial entry.
     */
    SUCCESS,

    /**
     * Payment gateway rejected or failed the charge.
     * Saga will trigger order compensation.
     */
    FAILED,

    /**
     * A successful payment was reversed.
     * Triggered during Saga compensation when Transaction Service fails
     * after a successful payment.
     */
    REFUNDED
}
