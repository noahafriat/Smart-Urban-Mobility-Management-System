package ca.concordia.summs.service;

import ca.concordia.summs.model.Vehicle;
import ca.concordia.summs.model.VehicleStatus;
import ca.concordia.summs.model.VehicleType;
import ca.concordia.summs.model.Car;
import ca.concordia.summs.pattern.factory.VehicleFactory;
import ca.concordia.summs.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    /**
     * Helper to search through available vehicles. Safely parses vehicle type.
     */
    public List<Vehicle> searchVehicles(String city, String typeStr) {
        VehicleType type = null;
        if (typeStr != null && !typeStr.isBlank()) {
            try {
                type = VehicleType.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Ignore invalid type; just don't filter by it
            }
        }
        return vehicleRepository.findAvailable(city, type);
    }

    public Optional<Vehicle> getVehicle(String id) {
        return vehicleRepository.findById(id);
    }

    public List<Vehicle> getProviderFleet(String providerId, String city, String typeStr, String statusStr, Boolean includeHidden) {
        VehicleType type = parseVehicleType(typeStr);
        VehicleStatus status = parseVehicleStatus(statusStr);

        return vehicleRepository.findByProviderId(providerId).stream()
                .filter(vehicle -> city == null || city.isBlank() || vehicle.getLocationCity().equalsIgnoreCase(city))
                .filter(vehicle -> type == null || vehicle.getType() == type)
                .filter(vehicle -> status == null || vehicle.getStatus() == status)
                .filter(vehicle -> Boolean.TRUE.equals(includeHidden) || vehicle.isVisibleInSearch())
                .toList();
    }

    public Map<String, Object> getProviderSummary(String providerId) {
        List<Vehicle> fleet = vehicleRepository.findByProviderId(providerId);
        Map<String, Long> byStatus = fleet.stream()
                .collect(Collectors.groupingBy(vehicle -> vehicle.getStatus().name(), TreeMap::new, Collectors.counting()));
        Map<String, Long> byType = fleet.stream()
                .collect(Collectors.groupingBy(vehicle -> {
                    if (vehicle.getType() == VehicleType.CAR && vehicle.getModel() != null) return vehicle.getModel();
                    return "Scooter";
                }, TreeMap::new, Collectors.counting()));
        Map<String, Long> byCity = fleet.stream()
                .collect(Collectors.groupingBy(Vehicle::getLocationCity, TreeMap::new, Collectors.counting()));

        long lowEnergyVehicles = fleet.stream().filter(this::isLowEnergy).count();
        long issueFlaggedVehicles = fleet.stream()
                .filter(vehicle -> vehicle.getStatus() == VehicleStatus.MAINTENANCE || !"READY".equalsIgnoreCase(vehicle.getMaintenanceState()))
                .count();
        long visibleVehicles = fleet.stream().filter(Vehicle::isVisibleInSearch).count();
        long inactiveVehicles = fleet.stream().filter(vehicle -> vehicle.getStatus() == VehicleStatus.INACTIVE).count();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalVehicles", fleet.size());
        summary.put("visibleVehicles", visibleVehicles);
        summary.put("inactiveVehicles", inactiveVehicles);
        summary.put("availableVehicles", byStatus.getOrDefault(VehicleStatus.AVAILABLE.name(), 0L));
        summary.put("rentedVehicles", byStatus.getOrDefault(VehicleStatus.RENTED.name(), 0L));
        summary.put("maintenanceVehicles", byStatus.getOrDefault(VehicleStatus.MAINTENANCE.name(), 0L));
        summary.put("lowEnergyVehicles", lowEnergyVehicles);
        summary.put("issueFlaggedVehicles", issueFlaggedVehicles);
        summary.put("byStatus", byStatus);
        summary.put("byType", byType);
        summary.put("byCity", byCity);
        return summary;
    }

    public Vehicle createVehicle(String providerId, Map<String, Object> payload) {
        VehicleType type = requireVehicleType(payload.get("type"));
        String city = requireText(payload.get("locationCity"), "Location city is required.");

        Vehicle vehicle = VehicleFactory.createVehicle(type, providerId, city);
        applyVehicleUpdates(vehicle, payload, false);
        applyDefaultCoordinatesIfMissing(vehicle, payload);
        if (vehicle.getStatus() == VehicleStatus.INACTIVE) {
            vehicle.setVisibleInSearch(false);
        }
        vehicleRepository.save(vehicle);
        return vehicle;
    }

    public Vehicle updateVehicle(String providerId, String vehicleId, Map<String, Object> payload) {
        Vehicle vehicle = getOwnedVehicle(providerId, vehicleId);
        applyVehicleUpdates(vehicle, payload, true);
        applyDefaultCoordinatesIfMissing(vehicle, payload);
        vehicleRepository.save(vehicle);
        return vehicle;
    }

    public Vehicle sendToMaintenance(String providerId, String vehicleId, Map<String, Object> payload) {
        Vehicle vehicle = getOwnedVehicle(providerId, vehicleId);
        ensureNotRented(vehicle, "Rented vehicles cannot be sent to maintenance.");
        vehicle.setStatus(VehicleStatus.MAINTENANCE);
        vehicle.setVisibleInSearch(false);
        vehicle.setMaintenanceState(readText(payload.get("maintenanceState"), "IN_SERVICE"));
        vehicleRepository.save(vehicle);
        return vehicle;
    }

    public Vehicle restoreFromMaintenance(String providerId, String vehicleId) {
        Vehicle vehicle = getOwnedVehicle(providerId, vehicleId);
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle.setMaintenanceState("READY");
        if (!vehicle.isRetired()) {
            vehicle.setVisibleInSearch(true);
        }
        vehicleRepository.save(vehicle);
        return vehicle;
    }

    public Vehicle relocateVehicle(String providerId, String vehicleId, Map<String, Object> payload) {
        Vehicle vehicle = getOwnedVehicle(providerId, vehicleId);
        ensureNotRented(vehicle, "Rented vehicles cannot be relocated.");

        String city = requireText(payload.get("locationCity"), "Destination city is required.");
        vehicle.setLocationCity(city);
        if (payload.containsKey("locationZone")) {
            vehicle.setLocationZone(readText(payload.get("locationZone"), vehicle.getLocationZone()));
        }
        if (payload.containsKey("latitude")) {
            vehicle.setLatitude(readDouble(payload.get("latitude"), vehicle.getLatitude()));
        }
        if (payload.containsKey("longitude")) {
            vehicle.setLongitude(readDouble(payload.get("longitude"), vehicle.getLongitude()));
        }
        applyDefaultCoordinatesIfMissing(vehicle, payload);
        vehicleRepository.save(vehicle);
        return vehicle;
    }

    public Vehicle deactivateVehicle(String providerId, String vehicleId) {
        Vehicle vehicle = getOwnedVehicle(providerId, vehicleId);
        ensureNotRented(vehicle, "Rented vehicles cannot be deactivated.");
        vehicle.setStatus(VehicleStatus.INACTIVE);
        vehicle.setVisibleInSearch(false);
        vehicle.setRetired(true);
        vehicle.setMaintenanceState("RETIRED");
        vehicleRepository.save(vehicle);
        return vehicle;
    }

    private Vehicle getOwnedVehicle(String providerId, String vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));
        if (!vehicle.getProviderId().equals(providerId)) {
            throw new IllegalArgumentException("Vehicle does not belong to this provider.");
        }
        return vehicle;
    }

    private void applyVehicleUpdates(Vehicle vehicle, Map<String, Object> payload, boolean allowTypeChange) {
        if (allowTypeChange && payload.containsKey("type")) {
            VehicleType requestedType = requireVehicleType(payload.get("type"));
            if (requestedType != vehicle.getType()) {
                throw new IllegalArgumentException("Vehicle type cannot be changed after creation.");
            }
        }

        if (payload.containsKey("vehicleCode")) {
            vehicle.setVehicleCode(requireText(payload.get("vehicleCode"), "Vehicle code is required."));
        }
        if (payload.containsKey("model")) {
            vehicle.setModel(requireText(payload.get("model"), "Model is required."));
        }
        if (payload.containsKey("locationCity")) {
            vehicle.setLocationCity(requireText(payload.get("locationCity"), "Location city is required."));
        }
        if (payload.containsKey("locationZone")) {
            vehicle.setLocationZone(requireText(payload.get("locationZone"), "Location zone is required."));
        }
        if (payload.containsKey("latitude")) {
            vehicle.setLatitude(readDouble(payload.get("latitude"), vehicle.getLatitude()));
        }
        if (payload.containsKey("longitude")) {
            vehicle.setLongitude(readDouble(payload.get("longitude"), vehicle.getLongitude()));
        }
        if (payload.containsKey("batteryLevel")) {
            vehicle.setBatteryLevel(readDouble(payload.get("batteryLevel"), vehicle.getBatteryLevel()));
        }
        if (payload.containsKey("fuelLevel")) {
            vehicle.setFuelLevel(readDouble(payload.get("fuelLevel"), vehicle.getFuelLevel()));
        }
        if (payload.containsKey("basePrice")) {
            vehicle.setBasePrice(readDouble(payload.get("basePrice"), vehicle.getBasePrice()));
        }
        if (payload.containsKey("pricePerMinute")) {
            vehicle.setPricePerMinute(readDouble(payload.get("pricePerMinute"), vehicle.getPricePerMinute()));
        }
        if (payload.containsKey("pricingCategory")) {
            vehicle.setPricingCategory(requireText(payload.get("pricingCategory"), "Pricing category is required."));
        }
        if (payload.containsKey("maintenanceState")) {
            vehicle.setMaintenanceState(requireText(payload.get("maintenanceState"), "Maintenance state is required."));
        }
        if (payload.containsKey("visibleInSearch")) {
            vehicle.setVisibleInSearch(Boolean.TRUE.equals(payload.get("visibleInSearch")));
        }
        if (payload.containsKey("status")) {
            VehicleStatus status = requireVehicleStatus(payload.get("status"));
            if (vehicle.getStatus() == VehicleStatus.RENTED && status != VehicleStatus.RENTED) {
                throw new IllegalArgumentException("Use the rental lifecycle to change a rented vehicle.");
            }
            vehicle.setStatus(status);
            if (status == VehicleStatus.MAINTENANCE) {
                vehicle.setVisibleInSearch(false);
            }
            if (status == VehicleStatus.INACTIVE) {
                vehicle.setRetired(true);
                vehicle.setVisibleInSearch(false);
            }
        }

        if (vehicle.getType() == VehicleType.CAR && vehicle instanceof Car car && payload.containsKey("licensePlate")) {
            car.setLicensePlate(requireText(payload.get("licensePlate"), "License plate is required for cars."));
        }

        if (vehicle.getType() != VehicleType.CAR) {
            vehicle.setFuelLevel(0.0);
        }
    }

    private void ensureNotRented(Vehicle vehicle, String message) {
        if (vehicle.getStatus() == VehicleStatus.RENTED || vehicle.getStatus() == VehicleStatus.RESERVED) {
            throw new IllegalArgumentException(message);
        }
    }

    private boolean isLowEnergy(Vehicle vehicle) {
        if (vehicle.getType() == VehicleType.CAR) {
            return vehicle.getFuelLevel() > 0.0 && vehicle.getFuelLevel() < 25.0;
        }
        return vehicle.getBatteryLevel() < 25.0;
    }

    private VehicleType requireVehicleType(Object value) {
        VehicleType type = parseVehicleType(value == null ? null : value.toString());
        if (type == null) {
            throw new IllegalArgumentException("Vehicle type is required.");
        }
        return type;
    }

    private VehicleStatus requireVehicleStatus(Object value) {
        VehicleStatus status = parseVehicleStatus(value == null ? null : value.toString());
        if (status == null) {
            throw new IllegalArgumentException("Vehicle status is invalid.");
        }
        return status;
    }

    private VehicleType parseVehicleType(String typeStr) {
        if (typeStr != null && !typeStr.isBlank()) {
            try {
                return VehicleType.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    private VehicleStatus parseVehicleStatus(String statusStr) {
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                return VehicleStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    private String requireText(Object value, String errorMessage) {
        String text = readText(value, null);
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return text;
    }

    private String readText(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = value.toString().trim();
        return text.isBlank() ? fallback : text;
    }

    private double readDouble(Object value, double fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    private void applyDefaultCoordinatesIfMissing(Vehicle vehicle, Map<String, Object> payload) {
        boolean hasLat = payload.containsKey("latitude");
        boolean hasLon = payload.containsKey("longitude");
        if (hasLat && hasLon) {
            return;
        }
        double[] coords = resolveCoordinates(vehicle.getLocationCity(), vehicle.getLocationZone());
        vehicle.setLatitude(coords[0]);
        vehicle.setLongitude(coords[1]);
    }

    private double[] resolveCoordinates(String city, String zone) {
        String key = city + "|" + zone;
        Map<String, double[]> known = Map.ofEntries(
                Map.entry("Montreal|Dock: McGill & Sherbrooke", new double[]{45.5049, -73.5757}),
                Map.entry("Montreal|Dock: Place des Arts", new double[]{45.5085, -73.5680}),
                Map.entry("Montreal|Dock: Guy-Concordia", new double[]{45.4959, -73.5790}),
                Map.entry("Montreal|Dock: Berri-UQAM", new double[]{45.5151, -73.5612}),
                Map.entry("Laval|Dock: Montmorency Station", new double[]{45.5582, -73.7244}),
                Map.entry("Laval|Dock: Centropolis", new double[]{45.5685, -73.7489}),
                Map.entry("Laval|Dock: Cartier Station", new double[]{45.5501, -73.7124}),
                Map.entry("Montreal|Station: Griffintown Surface Lot", new double[]{45.49202, -73.55642}),
                Map.entry("Montreal|Station: Griffintown Underground", new double[]{45.49200, -73.55636}),
                Map.entry("Montreal|FLEX Zone: Mile End", new double[]{45.52544, -73.59532}),
                Map.entry("Montreal|FLEX Zone: Old Montreal", new double[]{45.50761, -73.55427}),
                Map.entry("Laval|Station: Chomedey Reserved", new double[]{45.5515, -73.7565}),
                Map.entry("Laval|FLEX Zone: Laval-des-Rapides", new double[]{45.5572, -73.7025})
        );
        if (known.containsKey(key)) {
            return known.get(key);
        }
        if ("Laval".equalsIgnoreCase(city)) {
            return new double[]{45.5623, -73.7339};
        }
        return new double[]{45.5019, -73.5674};
    }
}
