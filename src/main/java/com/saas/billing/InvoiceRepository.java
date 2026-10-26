package com.saas.billing;

public interface InvoiceRepository {
    void save(String customerId, Money amount, String status);
}
