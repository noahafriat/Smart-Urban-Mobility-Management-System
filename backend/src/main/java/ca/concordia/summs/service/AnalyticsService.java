package ca.concordia.summs.service;

import ca.concordia.summs.model.Rental;
import ca.concordia.summs.model.RentalStatus;
import ca.concordia.summs.model.Vehicle;
import ca.concordia.summs.model.VehicleStatus;
import ca.concordia.summs.pattern.singleton.AnalyticsEngine;
import ca.concordia.summs.repository.RentalRepository;
import ca.concordia.summs.repository.UserRepository;
import ca.concordia.summs.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AnalyticsService drives UC-18, UC-19, and UC-20.
 * Reads from the in-memory repositories and the Singleton AnalyticsEngine
 * to produce platform-wide, rental, and parking/vehicle usage dashboards.
 */
@Service
public class AnalyticsService {

    private final RentalRepository rentalRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public AnalyticsService(RentalRepository rentalRepository,
                            VehicleRepository vehicleRepository,
                            UserRepository userRepository) {
        this.rentalRepository = rentalRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    /**
     * UC-18: Transit Usage Analytics (City Admin / System Admin only)
     * Returns a platform-wide snapshot of ridership patterns.
     */
    public Map<String, Object> getTransitAnalytics() {
        List<Rental> all = rentalRepository.findAll();
        List<Rental> completed = all.stream()
                .filter(r -> r.getStatus() == RentalStatus.COMPLETED || r.getStatus() == RentalStatus.PAID)
                .toList();

        // Total trips
        long totalTrips = completed.size();

        // Revenue
        double totalRevenue = completed.stream().mapToDouble(Rental::getTotalCost).sum();

        // Average trip duration in minutes
        double avgDurationMinutes = completed.stream()
                .filter(r -> r.getEndTime() != null)
                .mapToLong(r -> Duration.between(r.getStartTime(), r.getEndTime()).toMinutes())
                .average()
                .orElse(0.0);

        // Trips per vehicle type
        Map<String, Long> tripsByType = completed.stream()
                .collect(Collectors.groupingBy(r -> r.getVehicle().getType().name(),
                        TreeMap::new, Collectors.counting()));

        // Trips per city
        Map<String, Long> tripsByCity = completed.stream()
                .collect(Collectors.groupingBy(r -> r.getVehicle().getLocationCity(),
                        TreeMap::new, Collectors.counting()));

        // Fleet availability ratio
        List<Vehicle> fleet = vehicleRepository.findAll();
        long available = fleet.stream().filter(v -> v.getStatus() == VehicleStatus.AVAILABLE).count();
        long inUse = fleet.stream().filter(v -> v.getStatus() == VehicleStatus.RENTED).count();
        long maintenance = fleet.stream().filter(v -> v.getStatus() == VehicleStatus.MAINTENANCE).count();

        // Registered users count
        long totalUsers = userRepository.count();

        // Recent event logs from the Singleton AnalyticsEngine (Observer events)
        List<String> eventLog = AnalyticsEngine.getInstance().getEventLogs();
        List<String> recentEvents = eventLog.subList(Math.max(0, eventLog.size() - 20), eventLog.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalTrips", totalTrips);
        result.put("totalRevenue", Math.round(totalRevenue * 100.0) / 100.0);
        result.put("avgTripDurationMinutes", Math.round(avgDurationMinutes * 10.0) / 10.0);
        result.put("tripsByType", tripsByType);
        result.put("tripsByCity", tripsByCity);
        result.put("fleetAvailable", available);
        result.put("fleetInUse", inUse);
        result.put("fleetMaintenance", maintenance);
        result.put("totalUsers", totalUsers);
        result.put("recentEvents", recentEvents);
        return result;
    }

    /**
     * UC-19: Rental Service Analytics
     * - System/City Admin → sees all providers' data
     * - Provider → restricted to their own fleet and rentals only
     */
    public Map<String, Object> getRentalAnalytics(String providerId) {
        boolean isGlobal = (providerId == null || providerId.isBlank());

        List<Rental> allRentals = rentalRepository.findAll();
        List<Rental> scope = isGlobal ? allRentals : allRentals.stream()
                .filter(r -> r.getVehicle().getProviderId().equalsIgnoreCase(providerId))
                .toList();

        List<Rental> finished = scope.stream()
                .filter(r -> r.getStatus() == RentalStatus.COMPLETED || r.getStatus() == RentalStatus.PAID)
                .toList();

        long totalRentals = scope.size();
        long activeRentals = scope.stream().filter(r -> r.getStatus() == RentalStatus.ACTIVE).count();
        long completedRentals = finished.size();
        long paidRentals = scope.stream().filter(r -> r.getStatus() == RentalStatus.PAID).count();
        double totalRevenue = finished.stream().mapToDouble(Rental::getTotalCost).sum();

        // Revenue by vehicle type
        Map<String, Double> revenueByType = finished.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getVehicle().getType().name(),
                        TreeMap::new,
                        Collectors.summingDouble(Rental::getTotalCost)));
        revenueByType.replaceAll((k, v) -> Math.round(v * 100.0) / 100.0);

        // Rentals by vehicle type
        Map<String, Long> rentalsByType = scope.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getVehicle().getType().name(), TreeMap::new, Collectors.counting()));

        // Rentals by city
        Map<String, Long> rentalsByCity = scope.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getVehicle().getLocationCity(), TreeMap::new, Collectors.counting()));

        // Top rented vehicles (by vehicleCode)
        Map<String, Long> topVehicles = scope.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getVehicle().getVehicleCode(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scope", isGlobal ? "platform" : "provider:" + providerId);
        result.put("totalRentals", totalRentals);
        result.put("activeRentals", activeRentals);
        result.put("completedRentals", completedRentals);
        result.put("paidRentals", paidRentals);
        result.put("totalRevenue", Math.round(totalRevenue * 100.0) / 100.0);
        result.put("revenueByType", revenueByType);
        result.put("rentalsByType", rentalsByType);
        result.put("rentalsByCity", rentalsByCity);
        result.put("topVehicles", topVehicles);
        return result;
    }

    /**
     * UC-20: Parking / Fleet Utilization Analytics (Admin only)
     * Models parking-zone utilization via vehicle locations as a proxy.
     */
    public Map<String, Object> getParkingAnalytics() {
        List<Vehicle> fleet = vehicleRepository.findAll();

        // Vehicles parked (= available) per zone
        Map<String, Long> parkedPerZone = fleet.stream()
                .filter(v -> v.getStatus() == VehicleStatus.AVAILABLE)
                .collect(Collectors.groupingBy(
                        v -> v.getLocationCity() + " / " + v.getLocationZone(),
                        TreeMap::new, Collectors.counting()));

        // Occupancy per zone (total vehicles regardless of status)
        Map<String, Long> totalPerZone = fleet.stream()
                .filter(v -> v.getStatus() != VehicleStatus.INACTIVE)
                .collect(Collectors.groupingBy(
                        v -> v.getLocationCity() + " / " + v.getLocationZone(),
                        TreeMap::new, Collectors.counting()));

        // Occupancy rate per zone (% of zone capacity that is "in use")
        Map<String, String> occupancyRate = new TreeMap<>();
        totalPerZone.forEach((zone, total) -> {
            long available = parkedPerZone.getOrDefault(zone, 0L);
            long occupied = total - available;
            double pct = total == 0 ? 0 : (occupied * 100.0 / total);
            occupancyRate.put(zone, Math.round(pct) + "%");
        });

        // Vehicles in maintenance per city
        Map<String, Long> maintenancePerCity = fleet.stream()
                .filter(v -> v.getStatus() == VehicleStatus.MAINTENANCE)
                .collect(Collectors.groupingBy(Vehicle::getLocationCity, TreeMap::new, Collectors.counting()));

        // Overall utilization
        long totalActive = fleet.stream().filter(v -> v.getStatus() != VehicleStatus.INACTIVE).count();
        long totalAvailable = fleet.stream().filter(v -> v.getStatus() == VehicleStatus.AVAILABLE).count();
        double utilizationRate = totalActive == 0 ? 0.0 : ((totalActive - totalAvailable) * 100.0 / totalActive);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalVehicles", fleet.size());
        result.put("totalAvailableInZones", totalAvailable);
        result.put("overallUtilizationRate", Math.round(utilizationRate * 10.0) / 10.0 + "%");
        result.put("parkedPerZone", parkedPerZone);
        result.put("occupancyRate", occupancyRate);
        result.put("maintenancePerCity", maintenancePerCity);
        return result;
    }
}
