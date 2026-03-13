package ca.concordia.summs.pattern.factory;

import ca.concordia.summs.model.*;

import java.util.UUID;

// Factory Method pattern implementation for creating different vehicle types.
// Helps decouple the creation logic from the calling service.

public class VehicleFactory {

    public static Vehicle createVehicle(VehicleType type, String providerId, String locationCity) {
        switch (type) {
            case BIKE:
                return new Bike(providerId, locationCity);
            case SCOOTER:
                return new Scooter(providerId, locationCity);
            case CAR:
                // Generate a random mock license plate for cars
                String license = "QC-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
                return new Car(providerId, locationCity, license);
            default:
                throw new IllegalArgumentException("Unknown vehicle type: " + type);
        }
    }
}
