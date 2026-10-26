package com.saas.billing;

public interface PaymentGateway {
    boolean charge(String customerId, Money amount, String paymentCardToken);
}
