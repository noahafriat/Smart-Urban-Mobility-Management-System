package ca.concordia.summs.controller;

import ca.concordia.summs.model.ParkingReservation;
import ca.concordia.summs.service.ParkingReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parking-reservations")
public class ParkingReservationController {

    private final ParkingReservationService parkingReservationService;

    public ParkingReservationController(ParkingReservationService parkingReservationService) {
        this.parkingReservationService = parkingReservationService;
    }

    @PostMapping
    public ResponseEntity<Object> reserve(@RequestBody Map<String, Object> body) {
        try {
            if (body.get("userId") == null || body.get("garageId") == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "userId and garageId are required."));
            }
            String userId = String.valueOf(body.get("userId")).trim();
            String garageId = String.valueOf(body.get("garageId")).trim();
            if (userId.isEmpty() || garageId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "userId and garageId are required."));
            }
            int spots = 1;
            if (body.get("spots") != null) {
                if (body.get("spots") instanceof Number n) {
                    spots = n.intValue();
                } else {
                    spots = Integer.parseInt(String.valueOf(body.get("spots")).trim());
                }
            }
            Object paymentRaw = body.get("paymentInfo");
            String paymentInfo = paymentRaw == null ? "" : String.valueOf(paymentRaw).trim();
            boolean savePaymentMethod = false;
            if (body.get("savePaymentMethod") instanceof Boolean b) {
                savePaymentMethod = b;
            } else if (body.get("savePaymentMethod") != null) {
                savePaymentMethod = Boolean.parseBoolean(String.valueOf(body.get("savePaymentMethod")));
            }
            ParkingReservation r = parkingReservationService.reserve(userId, garageId, spots, paymentInfo, savePaymentMethod);
            return ResponseEntity.ok(Map.of(
                    "id", r.getId(),
                    "userId", r.getUserId(),
                    "garageId", r.getGarageId(),
                    "spots", r.getSpots(),
                    "status", r.getStatus().name(),
                    "startTime", r.getStartTime().toString(),
                    "paymentMethod", r.getReservationPaymentMethod(),
                    "paymentAmount", r.getReservationPaymentAmount(),
                    "paymentStatus", r.getReservationPaymentStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{reservationId}/complete")
    public ResponseEntity<Object> complete(@PathVariable String reservationId, @RequestBody Map<String, String> body) {
        try {
            String userId = body.get("userId");
            if (userId == null || userId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "userId is required."));
            }
            ParkingReservation r = parkingReservationService.complete(userId.trim(), reservationId);
            return ResponseEntity.ok(Map.of(
                    "id", r.getId(),
                    "status", r.getStatus().name(),
                    "endTime", r.getEndTime() != null ? r.getEndTime().toString() : ""
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<Object> cancel(@PathVariable String reservationId, @RequestBody Map<String, String> body) {
        try {
            String userId = body.get("userId");
            if (userId == null || userId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "userId is required."));
            }
            ParkingReservation r = parkingReservationService.cancel(userId.trim(), reservationId);
            return ResponseEntity.ok(Map.of(
                    "id", r.getId(),
                    "status", r.getStatus().name(),
                    "endTime", r.getEndTime() != null ? r.getEndTime().toString() : ""
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Object> listForUser(@PathVariable String userId) {
        return ResponseEntity.ok(parkingReservationService.listForUserEnriched(userId));
    }
}
