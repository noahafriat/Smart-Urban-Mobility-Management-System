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

    // Demo accounts

    private void seed() {
        User alice = new User("Alice Martin",   "alice@summs.ca", "password123", UserRole.CITIZEN);
        alice.setPreferredCity("Montreal");
        alice.setPreferredMobilityType("All");
        alice.setPaymentInfo("VISA-4242");
        save(alice);

        User admin = new User("City Admin",     "admin@summs.ca", "admin123",    UserRole.CITY_ADMIN);
        save(admin);

        User bixi  = new User("demo-provider-id", "BIXI Operator", "bixi@summs.ca", "bixi123", UserRole.MOBILITY_PROVIDER);
        save(bixi);

        User sys   = new User("System Admin",   "sys@summs.ca",   "sys123",      UserRole.SYSTEM_ADMIN);
        save(sys);
    }
}
