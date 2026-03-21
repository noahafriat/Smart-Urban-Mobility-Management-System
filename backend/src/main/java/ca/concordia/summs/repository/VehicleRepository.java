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
    private final Map<String, List<double[]>> zoneCoordinates = buildZoneCoordinates();

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
        assignCoordinates(brokenCar, "Laval", "FLEX Zone: Laval-des-Rapides", 99);
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
            assignCoordinates(v, city, zone, i);
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
            assignCoordinates(v, city, zone, i);
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

    private void assignCoordinates(Vehicle vehicle, String city, String zone, int index) {
        String key = city + "|" + zone;
        if (vehicle.getType() == VehicleType.SCOOTER && zone.startsWith("Dock:")) {
            assignDockClusterCoordinates(vehicle, key, city, index);
            return;
        }
        List<double[]> streetPoints = zoneCoordinates.get(key);
        if (streetPoints == null || streetPoints.isEmpty()) {
            double[] fallback = cityCenter(city);
            vehicle.setLatitude(fallback[0]);
            vehicle.setLongitude(fallback[1]);
            return;
        }
        int slot = Math.floorMod(index - 1, streetPoints.size());
        double[] point = streetPoints.get(slot);
        vehicle.setLatitude(point[0]);
        vehicle.setLongitude(point[1]);
    }

    private void assignDockClusterCoordinates(Vehicle vehicle, String key, String city, int index) {
        List<double[]> anchors = zoneCoordinates.get(key);
        double[] anchor = (anchors != null && !anchors.isEmpty()) ? anchors.get(0) : cityCenter(city);
        // Very small offsets keep scooters visually grouped as one dock.
        double[][] dockOffsets = new double[][]{
                {0.00000, 0.00000},
                {0.00003, 0.00002},
                {0.00003, -0.00002},
                {-0.00003, 0.00002},
                {-0.00003, -0.00002},
                {0.00005, 0.00000},
                {-0.00005, 0.00000},
                {0.00000, 0.00005}
        };
        int slot = Math.floorMod(index - 1, dockOffsets.length);
        double[] offset = dockOffsets[slot];
        vehicle.setLatitude(anchor[0] + offset[0]);
        vehicle.setLongitude(anchor[1] + offset[1]);
    }

    private Map<String, List<double[]>> buildZoneCoordinates() {
        Map<String, List<double[]>> map = new HashMap<>();
        map.put("Montreal|Dock: McGill & Sherbrooke", List.of(
                new double[]{45.5049, -73.5757},
                new double[]{45.5046, -73.5763},
                new double[]{45.5052, -73.5760}
        ));
        map.put("Montreal|Dock: Place des Arts", List.of(
                new double[]{45.5085, -73.5680},
                new double[]{45.5089, -73.5673},
                new double[]{45.5083, -73.5675}
        ));
        map.put("Montreal|Dock: Guy-Concordia", List.of(
                new double[]{45.4959, -73.5790},
                new double[]{45.4956, -73.5794},
                new double[]{45.4962, -73.5788}
        ));
        map.put("Montreal|Dock: Berri-UQAM", List.of(
                new double[]{45.5151, -73.5612},
                new double[]{45.5154, -73.5608},
                new double[]{45.5149, -73.5609}
        ));
        map.put("Laval|Dock: Montmorency Station", List.of(
                new double[]{45.5582, -73.7244},
                new double[]{45.5585, -73.7239},
                new double[]{45.5579, -73.7242}
        ));
        map.put("Laval|Dock: Centropolis", List.of(
                new double[]{45.5685, -73.7489},
                new double[]{45.5682, -73.7495},
                new double[]{45.5687, -73.7492}
        ));
        map.put("Laval|Dock: Cartier Station", List.of(
                new double[]{45.5501, -73.7124},
                new double[]{45.5504, -73.7119},
                new double[]{45.5498, -73.7120}
        ));
        map.put("Montreal|Station: Griffintown Surface Lot", List.of(
                new double[]{45.49202, -73.55642},
                new double[]{45.49196, -73.55655},
                new double[]{45.49208, -73.55631}
        ));
        map.put("Montreal|Station: Griffintown Underground", List.of(
                new double[]{45.49200, -73.55636},
                new double[]{45.49193, -73.55649},
                new double[]{45.49207, -73.55627}
        ));
        map.put("Montreal|FLEX Zone: Mile End", List.of(
                new double[]{45.52544, -73.59532},
                new double[]{45.52558, -73.59501},
                new double[]{45.52529, -73.59563}
        ));
        map.put("Montreal|FLEX Zone: Old Montreal", List.of(
                new double[]{45.50761, -73.55427},
                new double[]{45.50772, -73.55401},
                new double[]{45.50750, -73.55446}
        ));
        map.put("Laval|Station: Chomedey Reserved", List.of(
                new double[]{45.5515, -73.7565},
                new double[]{45.5518, -73.7560},
                new double[]{45.5513, -73.7561}
        ));
        map.put("Laval|FLEX Zone: Laval-des-Rapides", List.of(
                new double[]{45.5572, -73.7025},
                new double[]{45.5569, -73.7031},
                new double[]{45.5574, -73.7028}
        ));
        return map;
    }

    private double[] cityCenter(String city) {
        if ("Laval".equalsIgnoreCase(city)) {
            return new double[]{45.5623, -73.7339};
        }
        return new double[]{45.5019, -73.5674};
    }
}
