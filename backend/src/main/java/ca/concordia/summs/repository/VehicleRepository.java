package ca.concordia.summs.repository;

import ca.concordia.summs.model.Vehicle;
import ca.concordia.summs.model.VehicleStatus;
import ca.concordia.summs.model.VehicleType;
import ca.concordia.summs.pattern.factory.VehicleFactory;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// In-memory repository for Vehicle objects.
// Dummy fleet for now (might use external API later like CommuneAuto)

@Repository
public class VehicleRepository {

    private final Map<String, Vehicle> store = new ConcurrentHashMap<>();

    public VehicleRepository() {
        seed();
    }

    public void save(Vehicle vehicle) {
        store.put(vehicle.getId(), vehicle);
    }

    public Optional<Vehicle> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Vehicle> findAll() {
        return new ArrayList<>(store.values());
    }


    public List<Vehicle> findAvailable(String city, VehicleType type) {
        return store.values().stream()
                .filter(v -> v.getStatus() == VehicleStatus.AVAILABLE)
                .filter(v -> city == null || city.isBlank() || v.getLocationCity().equalsIgnoreCase(city))
                .filter(v -> type == null || v.getType() == type)
                .collect(Collectors.toList());
    }

    // Demo Data
    private void seed() {
        String bixiProviderId = "demo-provider-id";
        
        save(VehicleFactory.createVehicle(VehicleType.BIKE, bixiProviderId, "Montreal"));
        save(VehicleFactory.createVehicle(VehicleType.BIKE, bixiProviderId, "Montreal"));
        save(VehicleFactory.createVehicle(VehicleType.SCOOTER, bixiProviderId, "Montreal"));
        save(VehicleFactory.createVehicle(VehicleType.CAR, bixiProviderId, "Montreal"));

        save(VehicleFactory.createVehicle(VehicleType.BIKE, bixiProviderId, "Laval"));
        save(VehicleFactory.createVehicle(VehicleType.SCOOTER, bixiProviderId, "Laval"));
        save(VehicleFactory.createVehicle(VehicleType.CAR, bixiProviderId, "Laval"));
        save(VehicleFactory.createVehicle(VehicleType.CAR, bixiProviderId, "Laval"));
    }
}
