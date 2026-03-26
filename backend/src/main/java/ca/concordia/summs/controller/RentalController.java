package ca.concordia.summs.controller;

import ca.concordia.summs.service.RentalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/rentals")
public class RentalController {

    private final RentalService rentalService;

    public RentalController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    @PostMapping("/reserve")
    public ResponseEntity<Object> reserve(@RequestBody Map<String, String> body) {
        try {
            boolean savePaymentMethod = Boolean.parseBoolean(body.getOrDefault("savePaymentMethod", "false"));
            return ResponseEntity.ok(rentalService.reserveVehicle(
                    body.get("userId"),
                    body.get("vehicleId"),
                    body.get("paymentInfo"),
                    savePaymentMethod));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<Object> returnVehicle(@PathVariable String id) {
        try {
            return ResponseEntity.ok(rentalService.returnVehicle(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<Object> pay(@PathVariable String id) {
        try {
            return ResponseEntity.ok(rentalService.payForRental(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Object> getUserRentals(@PathVariable String userId) {
        return ResponseEntity.ok(rentalService.getUserRentals(userId));
    }
}
