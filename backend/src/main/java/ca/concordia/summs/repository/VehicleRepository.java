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
        String bixiProviderId = "demo-provider-id";

        Vehicle montrealBike = VehicleFactory.createVehicle(VehicleType.BIKE, bixiProviderId, "Montreal");
        montrealBike.setLocationZone("Plateau");
        montrealBike.setBatteryLevel(86.0);
        montrealBike.setVehicleCode("BIKE-MTL-001");
        save(montrealBike);

        Vehicle oldTownBike = VehicleFactory.createVehicle(VehicleType.BIKE, bixiProviderId, "Montreal");
        oldTownBike.setLocationZone("Old Port");
        oldTownBike.setBatteryLevel(19.0);
        oldTownBike.setVehicleCode("BIKE-MTL-002");
        save(oldTownBike);

        Vehicle montrealScooter = VehicleFactory.createVehicle(VehicleType.SCOOTER, bixiProviderId, "Montreal");
        montrealScooter.setLocationZone("Downtown");
        montrealScooter.setBatteryLevel(63.0);
        montrealScooter.setVehicleCode("SCOOT-MTL-003");
        save(montrealScooter);

        Vehicle montrealCar = VehicleFactory.createVehicle(VehicleType.CAR, bixiProviderId, "Montreal");
        montrealCar.setLocationZone("Griffintown");
        montrealCar.setBatteryLevel(0.0);
        montrealCar.setFuelLevel(72.0);
        montrealCar.setVehicleCode("CAR-MTL-004");
        save(montrealCar);

        Vehicle lavalBike = VehicleFactory.createVehicle(VehicleType.BIKE, bixiProviderId, "Laval");
        lavalBike.setLocationZone("Centropolis");
        lavalBike.setBatteryLevel(91.0);
        lavalBike.setVehicleCode("BIKE-LAV-005");
        save(lavalBike);

        Vehicle lavalScooter = VehicleFactory.createVehicle(VehicleType.SCOOTER, bixiProviderId, "Laval");
        lavalScooter.setLocationZone("Metro Laval");
        lavalScooter.setBatteryLevel(14.0);
        lavalScooter.setMaintenanceState("BATTERY_LOW");
        lavalScooter.setVehicleCode("SCOOT-LAV-006");
        save(lavalScooter);

        Vehicle lavalCar = VehicleFactory.createVehicle(VehicleType.CAR, bixiProviderId, "Laval");
        lavalCar.setLocationZone("Chomedey");
        lavalCar.setBatteryLevel(0.0);
        lavalCar.setFuelLevel(65.0);
        lavalCar.setStatus(VehicleStatus.MAINTENANCE);
        lavalCar.setMaintenanceState("BRAKE_CHECK");
        lavalCar.setVisibleInSearch(false);
        lavalCar.setVehicleCode("CAR-LAV-007");
        save(lavalCar);

        Vehicle retiredCar = VehicleFactory.createVehicle(VehicleType.CAR, bixiProviderId, "Laval");
        retiredCar.setLocationZone("Operations Hub");
        retiredCar.setBatteryLevel(0.0);
        retiredCar.setFuelLevel(28.0);
        retiredCar.setStatus(VehicleStatus.INACTIVE);
        retiredCar.setMaintenanceState("RETIRED");
        retiredCar.setVisibleInSearch(false);
        retiredCar.setRetired(true);
        retiredCar.setVehicleCode("CAR-LAV-008");
        save(retiredCar);
    }
}
