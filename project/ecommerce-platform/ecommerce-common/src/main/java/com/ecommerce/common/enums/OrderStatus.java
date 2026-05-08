package com.ecommerce.common.enums;

public enum OrderStatus {

    /**
     * Order has been submitted by customer.
     * Stock has NOT yet been reserved.
     * Payment has NOT yet been initiated.
     * Saga has NOT started yet.
     */
    PENDING,

    /**
     * Order validated and confirmed.
     * Stock has been reserved (reduced in product service).
     * Payment is being initiated.
     */
    CONFIRMED,

    /**
     * Payment has been received.
     * Transaction recording is in progress.
     */
    PROCESSING,

    /**
     * Payment step in Saga failed.
     * Stock reservation will be released (compensated).
     * Terminal failure state before compensation.
     */
    PAYMENT_FAILED,

    /**
     * Transaction recording step in Saga failed.
     * Payment refund will be initiated.
     * Stock reservation will be released.
     * Terminal failure state before compensation.
     */
    TRANSACTION_FAILED,

    /**
     * All Saga steps succeeded.
     * Payment received, transaction recorded.
     * Order is fulfilled. Terminal SUCCESS state.
     */
    COMPLETED,

    /**
     * Customer or admin explicitly cancelled the order.
     * Only allowed from PENDING state.
     * Terminal state.
     */
    CANCELLED,

    /**
     * Saga compensation completed.
     * All side effects (stock reservation, payment) have been reversed.
     * Terminal state after failure + compensation.
     */
    COMPENSATED
}
