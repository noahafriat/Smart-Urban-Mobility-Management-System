package ca.concordia.summs.service;

import ca.concordia.summs.model.ParkingGarage;
import ca.concordia.summs.model.User;
import ca.concordia.summs.model.UserRole;
import ca.concordia.summs.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ParkingGarageService {

    private final Map<String, ParkingGarage> garagesById = new ConcurrentHashMap<>();
    private final UserRepository userRepository;
    private final ParkingReservationService parkingReservationService;

    public ParkingGarageService(UserRepository userRepository,
                                @Lazy ParkingReservationService parkingReservationService) {
        this.userRepository = userRepository;
        this.parkingReservationService = parkingReservationService;
        seed();
    }

    private void seed() {
        addInternal(new ParkingGarage("pg1", ParkingGarage.CITY_INFRA_PROVIDER_ID, "Eaton Centre Parking",
                "705 Sainte-Catherine Ouest, Montreal", 45.5035, -73.5714, 250, 42));
        addInternal(new ParkingGarage("pg2", ParkingGarage.CITY_INFRA_PROVIDER_ID, "Quartier des Spectacles Auto-Parc",
                "1435 Rue de Bleury, Montreal", 45.5088, -73.5658, 400, 115));
        addInternal(new ParkingGarage("pg3", ParkingGarage.CITY_INFRA_PROVIDER_ID, "Old Port Clock Tower Parking",
                "1 Quai de l'Horloge, Montreal", 45.5103, -73.5484, 500, 320));
        addInternal(new ParkingGarage("pg4", ParkingGarage.CITY_INFRA_PROVIDER_ID, "Bell Centre Garage",
                "1225 Avenue des Canadiens-de-Montreal, Montreal", 45.4960, -73.5693, 300, 10));
        addInternal(new ParkingGarage("pg5", ParkingGarage.CITY_INFRA_PROVIDER_ID, "Plateau Station Parking",
                "4100 Rue Saint-Denis, Montreal", 45.5230, -73.5851, 150, 90));
    }

    private void addInternal(ParkingGarage g) {
        garagesById.put(g.getId(), g);
    }

    public List<ParkingGarage> getAllGarages() {
        return new ArrayList<>(garagesById.values());
    }

    public List<ParkingGarage> getGaragesForProvider(String providerId) {
        return garagesById.values().stream()
                .filter(g -> providerId.equals(g.getProviderId()))
                .toList();
    }

    public Optional<ParkingGarage> getById(String id) {
        return Optional.ofNullable(garagesById.get(id));
    }

    public ParkingGarage requireGarage(String id) {
        return getById(id).orElseThrow(() -> new IllegalArgumentException("Garage not found: " + id));
    }

    public Map<String, Object> getProviderSummary(String providerId) {
        List<ParkingGarage> mine = getGaragesForProvider(providerId);
        int totalCapacity = mine.stream().mapToInt(ParkingGarage::getTotalSpaces).sum();
        int totalAvailable = mine.stream().mapToInt(ParkingGarage::getAvailableSpaces).sum();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("garageCount", mine.size());
        m.put("totalSpaces", totalCapacity);
        m.put("availableSpaces", totalAvailable);
        m.put("occupiedSpaces", totalCapacity - totalAvailable);
        return m;
    }

    public ParkingGarage createGarage(String providerId, Map<String, Object> body) {
        requireParkingProvider(providerId);
        String name = requireString(body.get("name"), "name");
        String address = requireString(body.get("address"), "address");
        double lat = requireDouble(body.get("latitude"), "latitude");
        double lon = requireDouble(body.get("longitude"), "longitude");
        int total = requirePositiveInt(body.get("totalSpaces"), "totalSpaces");
        int available = body.get("availableSpaces") != null
                ? requireNonNegativeInt(body.get("availableSpaces"), "availableSpaces")
                : total;
        if (available > total) {
            throw new IllegalArgumentException("availableSpaces cannot exceed totalSpaces.");
        }
        String id = "pg-" + UUID.randomUUID();
        ParkingGarage g = new ParkingGarage(id, providerId, name, address, lat, lon, total, available);
        garagesById.put(id, g);
        return g;
    }

    public ParkingGarage updateGarage(String providerId, String garageId, Map<String, Object> body) {
        requireParkingProvider(providerId);
        ParkingGarage g = requireGarage(garageId);
        if (!providerId.equals(g.getProviderId())) {
            throw new IllegalArgumentException("This garage belongs to another provider.");
        }
        if (body.containsKey("name")) g.setName(String.valueOf(body.get("name")).trim());
        if (body.containsKey("address")) g.setAddress(String.valueOf(body.get("address")).trim());
        if (body.containsKey("latitude")) g.setLatitude(requireDouble(body.get("latitude"), "latitude"));
        if (body.containsKey("longitude")) g.setLongitude(requireDouble(body.get("longitude"), "longitude"));
        if (body.containsKey("totalSpaces")) {
            int newTotal = requirePositiveInt(body.get("totalSpaces"), "totalSpaces");
            int held = parkingReservationService.sumActiveSpotsForGarage(garageId);
            if (newTotal < held) {
                throw new IllegalArgumentException("totalSpaces cannot be less than active reservations (" + held + " spots).");
            }
            g.setTotalSpaces(newTotal);
            g.setAvailableSpaces(newTotal - held);
        }
        return g;
    }

    public void deleteGarage(String providerId, String garageId) {
        requireParkingProvider(providerId);
        ParkingGarage g = requireGarage(garageId);
        if (!providerId.equals(g.getProviderId())) {
            throw new IllegalArgumentException("This garage belongs to another provider.");
        }
        if (parkingReservationService.countActiveForGarage(garageId) > 0) {
            throw new IllegalStateException("Cannot delete a garage with active parking reservations.");
        }
        garagesById.remove(garageId);
    }

    private void requireParkingProvider(String providerId) {
        User u = userRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown user."));
        if (u.getRole() != UserRole.MOBILITY_PROVIDER || u.getProviderType() == null
                || !"PARKING".equalsIgnoreCase(u.getProviderType())) {
            throw new IllegalArgumentException("Only parking providers can manage garages.");
        }
    }

    private static String requireString(Object v, String field) {
        if (v == null) throw new IllegalArgumentException(field + " is required.");
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) throw new IllegalArgumentException(field + " is required.");
        return s;
    }

    private static double requireDouble(Object v, String field) {
        if (v == null) throw new IllegalArgumentException(field + " is required.");
        if (v instanceof Number n) return n.doubleValue();
        return Double.parseDouble(String.valueOf(v).trim());
    }

    private static int requirePositiveInt(Object v, String field) {
        int n = requireNonNegativeInt(v, field);
        if (n < 1) throw new IllegalArgumentException(field + " must be at least 1.");
        return n;
    }

    private static int requireNonNegativeInt(Object v, String field) {
        if (v == null) throw new IllegalArgumentException(field + " is required.");
        if (v instanceof Number n) return Math.max(0, n.intValue());
        return Integer.parseInt(String.valueOf(v).trim());
    }
}
