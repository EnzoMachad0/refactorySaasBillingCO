package com.saas.billing;

public interface NotificationService {
    void sendInvoice(Customer customer, Money amount);
}
