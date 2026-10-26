package com.saas.billing.infrastructure;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Configuração do banco por variáveis de ambiente, sem credenciais no código. */
public final class JdbcConnectionProvider {
    private final String url;
    private final String user;
    private final String password;

    public JdbcConnectionProvider(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public static JdbcConnectionProvider fromEnvironment() {
        return new JdbcConnectionProvider(required("BILLING_DB_URL"),
                required("BILLING_DB_USER"), required("BILLING_DB_PASSWORD"));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente ausente: " + name);
        }
        return value;
    }

    public Connection open() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
