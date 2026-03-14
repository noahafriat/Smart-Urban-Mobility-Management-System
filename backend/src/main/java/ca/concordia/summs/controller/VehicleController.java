package ca.concordia.summs.controller;

import ca.concordia.summs.service.VehicleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    /**
     * GET /api/vehicles/search?city=Montreal&type=BIKE
     * Story: Vehicle Search — filters the available fleet.
     */
    @GetMapping("/search")
    public ResponseEntity<Object> searchVehicles(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String type) {
        
        return ResponseEntity.ok(vehicleService.searchVehicles(city, type));
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<Object> getProviderFleet(
            @PathVariable String providerId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "true") Boolean includeHidden) {
        return ResponseEntity.ok(vehicleService.getProviderFleet(providerId, city, type, status, includeHidden));
    }

    @GetMapping("/provider/{providerId}/summary")
    public ResponseEntity<Object> getProviderSummary(@PathVariable String providerId) {
        return ResponseEntity.ok(vehicleService.getProviderSummary(providerId));
    }

    @PostMapping("/provider/{providerId}")
    public ResponseEntity<Object> createVehicle(@PathVariable String providerId, @RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(vehicleService.createVehicle(providerId, body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/provider/{providerId}/{vehicleId}")
    public ResponseEntity<Object> updateVehicle(
            @PathVariable String providerId,
            @PathVariable String vehicleId,
            @RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(vehicleService.updateVehicle(providerId, vehicleId, body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/provider/{providerId}/{vehicleId}/maintenance")
    public ResponseEntity<Object> sendToMaintenance(
            @PathVariable String providerId,
            @PathVariable String vehicleId,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            return ResponseEntity.ok(vehicleService.sendToMaintenance(providerId, vehicleId, body == null ? Map.of() : body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/provider/{providerId}/{vehicleId}/restore")
    public ResponseEntity<Object> restoreFromMaintenance(
            @PathVariable String providerId,
            @PathVariable String vehicleId) {
        try {
            return ResponseEntity.ok(vehicleService.restoreFromMaintenance(providerId, vehicleId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/provider/{providerId}/{vehicleId}/relocate")
    public ResponseEntity<Object> relocateVehicle(
            @PathVariable String providerId,
            @PathVariable String vehicleId,
            @RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(vehicleService.relocateVehicle(providerId, vehicleId, body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/provider/{providerId}/{vehicleId}/deactivate")
    public ResponseEntity<Object> deactivateVehicle(
            @PathVariable String providerId,
            @PathVariable String vehicleId) {
        try {
            return ResponseEntity.ok(vehicleService.deactivateVehicle(providerId, vehicleId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
