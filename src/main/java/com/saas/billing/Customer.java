package com.saas.billing;

import java.util.Objects;

public record Customer(String id, String email, String status) {
    public Customer {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(status, "status");
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }
}
