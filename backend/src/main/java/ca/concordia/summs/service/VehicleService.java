package ca.concordia.summs.service;

import ca.concordia.summs.model.Vehicle;
import ca.concordia.summs.model.VehicleType;
import ca.concordia.summs.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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
}
