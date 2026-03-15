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
    private String vehicleCode;
    private VehicleType type;
    private VehicleStatus status;
    private String locationCity;
    private String locationZone;
    private double batteryLevel;
    private double fuelLevel;
    private double basePrice;
    private double pricePerMinute;
    private String pricingCategory;
    private String maintenanceState;
    private boolean visibleInSearch;
    private boolean retired;
    private String model;

    public Vehicle(String providerId, VehicleType type, String locationCity) {
        this.id = UUID.randomUUID().toString();
        this.providerId = providerId;
        this.vehicleCode = type.name().charAt(0) + "-" + this.id.substring(0, 8).toUpperCase();
        this.type = type;
        this.status = VehicleStatus.AVAILABLE;
        this.locationCity = locationCity;
        this.locationZone = "Central";
        this.batteryLevel = 100.0;
        this.fuelLevel = 0.0;
        this.pricingCategory = "STANDARD";
        this.maintenanceState = "READY";
        this.visibleInSearch = true;
        this.retired = false;
        this.model = type == VehicleType.CAR ? "Standard Car" : "Standard Scooter";
    }
}
