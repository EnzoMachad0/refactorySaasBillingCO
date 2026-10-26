package com.saas.billing;

import java.math.BigDecimal;
import java.util.Objects;

/** Valor monetário imutável; não arredonda durante os cálculos. */
public record Money(BigDecimal amount) {
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        Objects.requireNonNull(amount, "amount");
    }

    public static Money of(double amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    public Money add(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money subtract(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    public Money multiply(String factor) {
        return new Money(amount.multiply(new BigDecimal(factor)));
    }

    public Money atLeastZero() {
        return amount.signum() < 0 ? ZERO : this;
    }
}
