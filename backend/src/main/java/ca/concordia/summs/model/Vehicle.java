package ca.concordia.summs.model;

import lombok.Data;
import java.util.UUID;

/**
 * Abstract base class for all vehicles. 
 * This is the Product interface for the Factory Method pattern.
 */
@Data
public abstract class Vehicle {
    private final String id;
    private final String providerId;
    private VehicleType type;
    private VehicleStatus status;
    private String locationCity;
    private double batteryLevel; // Used universally for e-bikes, scooters, and EVs
    private double basePrice;
    private double pricePerMinute;

    public Vehicle(String providerId, VehicleType type, String locationCity) {
        this.id = UUID.randomUUID().toString();
        this.providerId = providerId;
        this.type = type;
        this.status = VehicleStatus.AVAILABLE;
        this.locationCity = locationCity;
        this.batteryLevel = 100.0; // Default full battery
    }
}
