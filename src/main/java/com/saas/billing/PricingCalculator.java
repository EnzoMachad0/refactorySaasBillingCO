package com.saas.billing;

import java.time.LocalDate;
import java.util.Objects;

/** Regras de preço sem acesso a banco, pagamento ou rede. */
public final class PricingCalculator {
    public Money calculate(BillingCommand command, LocalDate today) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(today, "today");

        int monthsActive = monthsActive(command.signupDate(), today);
        Money price = applyLoyaltyDiscount(command.basePrice(), monthsActive);
        price = applyNonProfitDiscount(price, command.nonProfit());
        price = applyPromotion(price, command.promoCode(), monthsActive, today);
        return applyLateCharges(price, command.overdueDays()).atLeastZero();
    }

    private int monthsActive(LocalDate signupDate, LocalDate today) {
        return (today.getYear() - signupDate.getYear()) * 12
                + today.getMonthValue() - signupDate.getMonthValue();
    }

    private Money applyLoyaltyDiscount(Money basePrice, int monthsActive) {
        if (monthsActive > 24) {
            return basePrice.subtract(basePrice.multiply("0.15"));
        }
        if (monthsActive > 12) {
            return basePrice.subtract(basePrice.multiply("0.10"));
        }
        return basePrice;
    }

    private Money applyNonProfitDiscount(Money price, boolean nonProfit) {
        return nonProfit ? price.multiply("0.70") : price;
    }

    private Money applyPromotion(Money price, String promoCode, int monthsActive, LocalDate today) {
        if (promoCode == null || promoCode.trim().isEmpty()) {
            return price;
        }
        if (promoCode.equalsIgnoreCase("BLACKFRIDAY") && today.getMonthValue() == 11) {
            return price.subtract(Money.of(30));
        }
        if (promoCode.equalsIgnoreCase("WELCOME10") && monthsActive <= 1) {
            return price.multiply("0.90");
        }
        return price;
    }

    private Money applyLateCharges(Money price, int overdueDays) {
        if (overdueDays <= 0) {
            return price;
        }
        Money penalty = price.multiply("0.02");
        Money interest = price.multiply("0.001")
                .multiply(Integer.toString(overdueDays));
        return price.add(penalty).add(interest);
    }
}
