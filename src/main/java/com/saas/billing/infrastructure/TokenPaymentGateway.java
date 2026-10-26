package com.saas.billing.infrastructure;

import com.saas.billing.Money;
import com.saas.billing.PaymentGateway;

/** Reproduz a validação simulada de token do código original. */
public final class TokenPaymentGateway implements PaymentGateway {
    @Override
    public boolean charge(String customerId, Money amount, String paymentCardToken) {
        return paymentCardToken != null && paymentCardToken.startsWith("tok_");
    }
}
