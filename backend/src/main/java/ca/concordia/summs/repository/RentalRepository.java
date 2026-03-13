package ca.concordia.summs.repository;

import ca.concordia.summs.model.Rental;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class RentalRepository {
    private final Map<String, Rental> store = new ConcurrentHashMap<>();

    public void save(Rental rental) {
        store.put(rental.getId(), rental);
    }

    public Optional<Rental> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Rental> findByUserId(String userId) {
        return store.values().stream()
                .filter(r -> r.getUserId().equals(userId))
                // Sort by newest first
                .sorted((a, b) -> b.getStartTime().compareTo(a.getStartTime()))
                .collect(Collectors.toList());
    }
    
    public List<Rental> findAll() {
        return new ArrayList<>(store.values());
    }
}
