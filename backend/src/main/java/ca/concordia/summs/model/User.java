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

    // Simulated payment — no real gateway for Phase 3
    private String paymentInfo;

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
}
