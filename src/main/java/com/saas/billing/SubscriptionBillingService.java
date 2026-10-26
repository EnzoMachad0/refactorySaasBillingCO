package com.saas.billing;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SubscriptionBillingService {
    private final CustomerRepository customers;
    private final PaymentGateway payments;
    private final InvoiceRepository invoices;
    private final NotificationService notifications;
    private final PricingCalculator pricing;
    private final Clock clock;

    public SubscriptionBillingService(CustomerRepository customers, PaymentGateway payments,
            InvoiceRepository invoices, NotificationService notifications,
            PricingCalculator pricing, Clock clock) {
        this.customers = Objects.requireNonNull(customers, "customers");
        this.payments = Objects.requireNonNull(payments, "payments");
        this.invoices = Objects.requireNonNull(invoices, "invoices");
        this.notifications = Objects.requireNonNull(notifications, "notifications");
        this.pricing = Objects.requireNonNull(pricing, "pricing");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public List<String> processMonthlyBilling(BillingCommand command) {
        Objects.requireNonNull(command, "command");
        List<String> logs = new ArrayList<>();
        LocalDate today = LocalDate.now(clock);
        Money amount = pricing.calculate(command, today);
        customers.findById(command.customerId())
                .ifPresent(customer -> billCustomer(command, customer, amount, logs));
        return logs;
    }

    private void billCustomer(BillingCommand command, Customer customer,
            Money amount, List<String> logs) {
        if (!customer.isActive()) {
            logs.add("Cliente inativo. Cobrança cancelada: " + command.customerId());
            return;
        }

        boolean paid = payments.charge(customer.id(), amount, command.paymentCardToken());
        if (paid) {
            logs.add("Pagamento processado com sucesso: R$ " + amount.amount().doubleValue());
        } else {
            logs.add("Falha no pagamento do cartão para: " + customer.id());
        }

        invoices.save(customer.id(), amount, paid ? "PAID" : "FAILED");
        notifications.sendInvoice(customer, amount);
        logs.add("E-mail disparado para: " + customer.email());
    }

    /** Mantém a assinatura usada pelo código original. */
    public List<String> processMonthlyBilling(String customerId, String planType, double basePrice,
            String signupDateStr, int overdueDays, String promoCode,
            boolean isNonProfit, String paymentCardToken) {
        BillingCommand command = new BillingCommand(customerId, planType, Money.of(basePrice),
                LocalDate.parse(signupDateStr), overdueDays, promoCode,
                isNonProfit, paymentCardToken);
        return processMonthlyBilling(command);
    }
}
