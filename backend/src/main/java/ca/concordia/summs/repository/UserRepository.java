package ca.concordia.summs.repository;
import ca.concordia.summs.model.User;
import ca.concordia.summs.model.UserRole;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// In-memory repository for User objects (simulated database)
// Right now it has four demo accounts (one per role) for testing

@Repository
public class UserRepository {

    /** Stable id for the seeded city admin — municipal garages use this as {@code ParkingGarage#providerId}. */
    public static final String CITY_ADMIN_USER_ID = "city-admin-id";

    private final Map<String, User> store = new ConcurrentHashMap<>();

    public UserRepository() {
        seed();
    }

    // public apis

    public void save(User user) {
        store.put(user.getId(), user);
    }

    public Optional<User> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public Optional<User> findByEmail(String email) {
        return store.values().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    public List<User> findAll() {
        return new ArrayList<>(store.values());
    }

    public long count() {
        return store.size();
    }

    public boolean deleteById(String id) {
        return store.remove(id) != null;
    }

    // Demo accounts

    private void seed() {
        User user = new User("Test Account",   "noahafri12@gmail.com", "password123", UserRole.CITIZEN);
        user.setPaymentInfo("VISA-4242");
        save(user);

        User admin = new User(CITY_ADMIN_USER_ID, "City Admin", "admin@summs.ca", "admin123", UserRole.CITY_ADMIN);
        save(admin);

        User carProvider = new User("car-provider-id", "Car Provider", "cars@summs.ca", "cars123", UserRole.MOBILITY_PROVIDER);
        carProvider.setProviderType("CAR");
        save(carProvider);

        User scooterProvider = new User("scooter-provider-id", "Scooter Provider", "scooters@summs.ca", "scooters123", UserRole.MOBILITY_PROVIDER);
        scooterProvider.setProviderType("SCOOTER");
        save(scooterProvider);

        User parkingProvider = new User("parking-provider-id", "Parking Provider", "parking@summs.ca", "parking123", UserRole.MOBILITY_PROVIDER);
        parkingProvider.setProviderType("PARKING");
        save(parkingProvider);

        User sys   = new User("System Admin",   "sys@summs.ca",   "sys123",      UserRole.SYSTEM_ADMIN);
        save(sys);
    }
}
