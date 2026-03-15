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
                .filter(Vehicle::isVisibleInSearch)
                .filter(v -> !v.isRetired())
                .filter(v -> city == null || city.isBlank() || v.getLocationCity().equalsIgnoreCase(city))
                .filter(v -> type == null || v.getType() == type)
                .collect(Collectors.toList());
    }

    public List<Vehicle> findByProviderId(String providerId) {
        return store.values().stream()
                .filter(v -> v.getProviderId().equals(providerId))
                .sorted(Comparator.comparing(Vehicle::getLocationCity)
                        .thenComparing(Vehicle::getType)
                        .thenComparing(Vehicle::getVehicleCode))
                .collect(Collectors.toList());
    }

    // Demo Data
    private void seed() {
        String scooterProviderId = "scooter-provider-id";
        String carProviderId = "car-provider-id";

        seedScooterDock(scooterProviderId, "Montreal", "Dock: McGill & Sherbrooke", 12, "SCOOT-MTL-MG", 0.30);
        seedScooterDock(scooterProviderId, "Montreal", "Dock: Place des Arts", 8, "SCOOT-MTL-PA", 0.30);
        seedScooterDock(scooterProviderId, "Montreal", "Dock: Guy-Concordia", 15, "SCOOT-MTL-GC", 0.25);
        seedScooterDock(scooterProviderId, "Montreal", "Dock: Berri-UQAM", 20, "SCOOT-MTL-BU", 0.35);

        seedScooterDock(scooterProviderId, "Laval", "Dock: Montmorency Station", 10, "SCOOT-LAV-MM", 0.25);
        seedScooterDock(scooterProviderId, "Laval", "Dock: Centropolis", 14, "SCOOT-LAV-CP", 0.30);
        seedScooterDock(scooterProviderId, "Laval", "Dock: Cartier Station", 6, "SCOOT-LAV-CT", 0.25);

        seedCarZone(carProviderId, "Montreal", "Station: Griffintown Surface Lot", "Honda Civic", "Economy", 15.00, 0.40, 5, "CAR-MTL-GF");
        seedCarZone(carProviderId, "Montreal", "Station: Griffintown Underground", "Tesla Model 3", "Premium", 30.00, 1.20, 2, "CAR-MTL-GF-TSL");
        seedCarZone(carProviderId, "Montreal", "FLEX Zone: Mile End", "Toyota Corolla", "Economy", 15.00, 0.40, 4, "CAR-MTL-ME");
        seedCarZone(carProviderId, "Montreal", "FLEX Zone: Old Montreal", "BMW 3 Series", "Luxury", 40.00, 1.50, 3, "CAR-MTL-OM-BMW");

        seedCarZone(carProviderId, "Laval", "Station: Chomedey Reserved", "Honda Civic", "Economy", 18.00, 0.45, 6, "CAR-LAV-CH");
        seedCarZone(carProviderId, "Laval", "Station: Chomedey Reserved", "Ford F-150", "Truck", 35.00, 1.00, 2, "CAR-LAV-CH-TRK");
        seedCarZone(carProviderId, "Laval", "FLEX Zone: Laval-des-Rapides", "Hyundai Elantra", "Economy", 15.00, 0.35, 4, "CAR-LAV-LR");

        // A single car in maintenance
        Vehicle brokenCar = VehicleFactory.createVehicle(VehicleType.CAR, carProviderId, "Laval");
        brokenCar.setLocationZone("FLEX Zone: Laval-des-Rapides");
        brokenCar.setModel("Toyota Camry");
        brokenCar.setPricingCategory("Economy");
        brokenCar.setBasePrice(15.00);
        brokenCar.setPricePerMinute(0.40);
        brokenCar.setFuelLevel(25.0);
        brokenCar.setStatus(VehicleStatus.MAINTENANCE);
        brokenCar.setMaintenanceState("BRAKE_CHECK");
        brokenCar.setVisibleInSearch(false);
        brokenCar.setVehicleCode("CAR-LAV-00X");
        save(brokenCar);
    }

    private void seedScooterDock(String providerId, String city, String zone, int count, String prefix, double pricePerMin) {
        for (int i = 1; i <= count; i++) {
            Vehicle v = VehicleFactory.createVehicle(VehicleType.SCOOTER, providerId, city);
            v.setLocationZone(zone);
            v.setPricingCategory("Standard Scooter");
            v.setBasePrice(1.00); // Standard $1 unlock
            v.setPricePerMinute(pricePerMin);
            
            // Randomize battery realistically between 30% and 100%
            double battery = 30.0 + (Math.random() * 70.0);
            v.setBatteryLevel(battery);
            
            v.setVehicleCode(String.format("%s-%03d", prefix, i));
            save(v);
        }
    }

    private void seedCarZone(String providerId, String city, String zone, String model, String category, double basePrice, double pricePerMin, int count, String prefix) {
        for (int i = 1; i <= count; i++) {
            Vehicle v = VehicleFactory.createVehicle(VehicleType.CAR, providerId, city);
            v.setLocationZone(zone);
            v.setModel(model);
            v.setPricingCategory(category);
            v.setBasePrice(basePrice);
            v.setPricePerMinute(pricePerMin);
            
            // Randomize fuel realistically between 20% and 100%
            double fuel = 20.0 + (Math.random() * 80.0);
            v.setFuelLevel(fuel);
            
            v.setVehicleCode(String.format("%s-%03d", prefix, i));
            save(v);
        }
    }
}
