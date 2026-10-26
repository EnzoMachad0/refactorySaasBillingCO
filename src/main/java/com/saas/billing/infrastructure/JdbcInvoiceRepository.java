package com.saas.billing.infrastructure;

import com.saas.billing.BillingException;
import com.saas.billing.InvoiceRepository;
import com.saas.billing.Money;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class JdbcInvoiceRepository implements InvoiceRepository {
    private final JdbcConnectionProvider connections;

    public JdbcInvoiceRepository(JdbcConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public void save(String customerId, Money amount, String status) {
        String sql = "INSERT INTO invoices(customer_id, amount, status, created_at) "
                + "VALUES (?, ?, ?, NOW())";
        try (Connection connection = connections.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customerId);
            statement.setBigDecimal(2, amount.amount());
            statement.setString(3, status);
            statement.executeUpdate();
        } catch (SQLException cause) {
            throw new BillingException("Falha ao salvar fatura de " + customerId, cause);
        }
    }
}
