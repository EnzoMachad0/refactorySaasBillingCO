package com.saas.billing;

import java.time.LocalDate;
import java.util.Objects;

/** Dados necessários para uma cobrança mensal. */
public record BillingCommand(
        String customerId,
        String planType,
        Money basePrice,
        LocalDate signupDate,
        int overdueDays,
        String promoCode,
        boolean nonProfit,
        String paymentCardToken) {

    public BillingCommand {
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(basePrice, "basePrice");
        Objects.requireNonNull(signupDate, "signupDate");
    }
}
