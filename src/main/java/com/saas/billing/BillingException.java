package com.saas.billing;

/** Falha externa durante a cobrança, com a causa original preservada. */
public class BillingException extends RuntimeException {
    public BillingException(String message, Throwable cause) {
        super(message, cause);
    }
}
