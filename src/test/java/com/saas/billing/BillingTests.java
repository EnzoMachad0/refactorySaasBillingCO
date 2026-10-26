package com.saas.billing;

import com.saas.billing.infrastructure.EmailNotificationService;
import com.saas.billing.infrastructure.JdbcConnectionProvider;
import com.saas.billing.infrastructure.JdbcCustomerRepository;
import com.saas.billing.infrastructure.TokenPaymentGateway;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Testes executáveis sem bibliotecas externas: java -ea com.saas.billing.BillingTests. */
public final class BillingTests {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC);

    public static void main(String[] args) {
        testPricing();
        testBillingFlow();
        testMissingAndInactiveCustomers();
        testDatabaseCause();
        System.out.println("4 testes passaram");
    }

    private static void testPricing() {
        PricingCalculator pricing = new PricingCalculator();
        BillingCommand command = new BillingCommand("1", "basic", Money.of(100),
                LocalDate.of(2024, 10, 1), 10, "BLACKFRIDAY", true, "tok_ok");
        // 100 - 15%, menos 30% de ONG, menos 30, mais 2% e 10 dias a 0,1%.
        checkAmount("30.385", pricing.calculate(command, LocalDate.now(CLOCK)));
        checkAmount("0", pricing.calculate(new BillingCommand("1", "basic", Money.of(10),
                LocalDate.of(2026, 11, 1), 0, "BLACKFRIDAY", false, "tok_ok"),
                LocalDate.now(CLOCK)));
        checkAmount("90", pricing.calculate(new BillingCommand("1", "basic", Money.of(100),
                LocalDate.of(2026, 10, 1), 0, "WELCOME10", false, "tok_ok"),
                LocalDate.now(CLOCK)));
    }

    private static void testBillingFlow() {
        List<String> invoices = new ArrayList<>();
        List<String> emails = new ArrayList<>();
        SubscriptionBillingService service = new SubscriptionBillingService(
                id -> Optional.of(new Customer(id, "a@example.com", "ACTIVE")),
                new TokenPaymentGateway(),
                (id, amount, status) -> invoices.add(status),
                new EmailNotificationService((to, subject, body) -> emails.add(to)),
                new PricingCalculator(), CLOCK);

        List<String> success = service.processMonthlyBilling("1", "basic", 100,
                "2026-11-01", 0, null, false, "tok_ok");
        check(success.size() == 2 && success.get(0).contains("100.0"), "sucesso e valor");
        check(invoices.equals(List.of("PAID")) && emails.equals(List.of("a@example.com")),
                "fatura paga e notificação");

        List<String> failure = service.processMonthlyBilling("1", "basic", 100,
                "2026-11-01", 0, null, false, "invalid");
        check(failure.get(0).startsWith("Falha no pagamento"), "falha de cartão");
        check(invoices.equals(List.of("PAID", "FAILED")) && emails.size() == 2,
                "fatura falha e notificação");
    }

    private static void testMissingAndInactiveCustomers() {
        BillingCommand command = new BillingCommand("1", "basic", Money.of(100),
                LocalDate.of(2026, 11, 1), 0, null, false, "tok_ok");
        SubscriptionBillingService missing = service(id -> Optional.empty());
        check(missing.processMonthlyBilling(command).isEmpty(), "cliente ausente");
        SubscriptionBillingService inactive = service(id -> Optional.of(
                new Customer(id, "a@example.com", "INACTIVE")));
        check(inactive.processMonthlyBilling(command).equals(List.of(
                "Cliente inativo. Cobrança cancelada: 1")), "cliente inativo");
    }

    private static void testDatabaseCause() {
        JdbcCustomerRepository repository = new JdbcCustomerRepository(
                new JdbcConnectionProvider("jdbc:invalid:database", "user", "password"));
        try {
            repository.findById("1");
            throw new AssertionError("falha de banco esperada");
        } catch (BillingException expected) {
            check(expected.getCause() instanceof SQLException, "causa SQL preservada");
        }
    }

    private static SubscriptionBillingService service(CustomerRepository customers) {
        return new SubscriptionBillingService(customers, new TokenPaymentGateway(),
                (id, amount, status) -> { }, (customer, amount) -> { },
                new PricingCalculator(), CLOCK);
    }

    private static void checkAmount(String expected, Money actual) {
        check(actual.amount().compareTo(new BigDecimal(expected)) == 0,
                "valor esperado " + expected + ", obtido " + actual.amount());
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
