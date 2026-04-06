package ca.concordia.summs.controller;

import ca.concordia.summs.model.ParkingGarage;
import ca.concordia.summs.service.ParkingGarageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parking-garages")
public class ParkingGarageController {

    private final ParkingGarageService parkingGarageService;

    public ParkingGarageController(ParkingGarageService parkingGarageService) {
        this.parkingGarageService = parkingGarageService;
    }

    /** All garages (city + every provider) — map, citizen list, analytics. */
    @GetMapping
    public List<ParkingGarage> getAllGarages() {
        return parkingGarageService.getAllGarages();
    }

    @GetMapping("/provider/{providerId}")
    public List<ParkingGarage> getProviderGarages(@PathVariable String providerId) {
        return parkingGarageService.getGaragesForProvider(providerId);
    }

    @GetMapping("/provider/{providerId}/summary")
    public ResponseEntity<Object> getProviderSummary(@PathVariable String providerId) {
        return ResponseEntity.ok(parkingGarageService.getProviderSummary(providerId));
    }

    @PostMapping("/provider/{providerId}")
    public ResponseEntity<Object> createGarage(@PathVariable String providerId, @RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(parkingGarageService.createGarage(providerId, body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/provider/{providerId}/{garageId}")
    public ResponseEntity<Object> updateGarage(
            @PathVariable String providerId,
            @PathVariable String garageId,
            @RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(parkingGarageService.updateGarage(providerId, garageId, body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/provider/{providerId}/{garageId}")
    public ResponseEntity<Object> deleteGarage(@PathVariable String providerId, @PathVariable String garageId) {
        try {
            parkingGarageService.deleteGarage(providerId, garageId);
            return ResponseEntity.ok(Map.of("message", "Garage removed."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
