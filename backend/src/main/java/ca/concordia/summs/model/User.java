package ca.concordia.summs.model;

import lombok.Data;
import java.util.UUID;

/**
 * Represents a registered SUMMS user.
 * Supports all four roles: CITIZEN, MOBILITY_PROVIDER, CITY_ADMIN, SYSTEM_ADMIN.
 */
@Data
public class User {

    private final String id;
    private String name;
    private String email;
    private String password;
    private UserRole role;

    // Optional profile fields
    private String phone;
    private String preferredCity;
    private String preferredMobilityType;

    // Simulated payment — supports multiple fake cards
    private java.util.List<String> paymentMethods = new java.util.ArrayList<>();

    public User(String name, String email, String password, UserRole role) {
        this(UUID.randomUUID().toString(), name, email, password, role);
    }

    public User(String id, String name, String email, String password, UserRole role) {
        this.id       = id;
        this.name     = name;
        this.email    = email;
        this.password = password;
        this.role     = role;
    }

    // Helper for legacy code that only knows about one card
    public String getPaymentInfo() {
        return paymentMethods.isEmpty() ? null : paymentMethods.get(0);
    }

    public void setPaymentInfo(String method) {
        if (method != null && !method.isBlank()) {
           if (!paymentMethods.contains(method)) {
               paymentMethods.clear(); // for set, we replace or just keep one
               paymentMethods.add(method);
           }
        }
    }
}
