package com.saas.billing.infrastructure;

import com.saas.billing.BillingException;
import com.saas.billing.Customer;
import com.saas.billing.CustomerRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class JdbcCustomerRepository implements CustomerRepository {
    private final JdbcConnectionProvider connections;

    public JdbcCustomerRepository(JdbcConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public Optional<Customer> findById(String customerId) {
        String sql = "SELECT email, status FROM customers WHERE id = ?";
        try (Connection connection = connections.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customerId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(new Customer(customerId,
                            result.getString("email"), result.getString("status")));
                }
                return Optional.empty();
            }
        } catch (SQLException cause) {
            throw new BillingException("Falha ao consultar cliente " + customerId, cause);
        }
    }
}
