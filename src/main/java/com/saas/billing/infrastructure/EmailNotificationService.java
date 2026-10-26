package com.saas.billing.infrastructure;

import com.saas.billing.Customer;
import com.saas.billing.Money;
import com.saas.billing.NotificationService;
import java.util.Objects;

public final class EmailNotificationService implements NotificationService {
    private final EmailSender sender;

    public EmailNotificationService(EmailSender sender) {
        this.sender = Objects.requireNonNull(sender, "sender");
    }

    @Override
    public void sendInvoice(Customer customer, Money amount) {
        sender.send(customer.email(), "Sua Fatura Mensal",
                "Olá! Sua fatura foi processada no valor de R$ " + amount.amount().doubleValue());
    }
}
