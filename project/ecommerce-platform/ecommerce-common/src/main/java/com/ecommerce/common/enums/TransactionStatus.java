package com.ecommerce.common.enums;

public enum TransactionStatus {

    /**
     * Transaction record creation has started.
     * Financial entry is being prepared.
     */
    INITIATED,

    /**
     * Transaction record successfully written to the financial ledger.
     * This is the final SUCCESS state. Immutable in normal operations.
     */
    COMPLETED,

    /**
     * Transaction recording failed due to a system error.
     * Saga compensation will initiate payment refund.
     */
    FAILED,

    /**
     * A completed transaction has been reversed/voided.
     * Occurs ONLY during Saga compensation after all steps have succeeded
     * but a downstream verification fails. Very rare in practice.
     */
    REVERSED
}
