package com.saas.billing.infrastructure;

@FunctionalInterface
public interface EmailSender {
    void send(String recipient, String subject, String body);
}
