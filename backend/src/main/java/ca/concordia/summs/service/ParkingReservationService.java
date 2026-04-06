package ca.concordia.summs.service;

import ca.concordia.summs.model.ParkingGarage;
import ca.concordia.summs.model.ParkingReservation;
import ca.concordia.summs.model.ParkingReservationStatus;
import ca.concordia.summs.model.User;
import ca.concordia.summs.model.UserRole;
import ca.concordia.summs.pattern.strategy.PaymentStrategy;
import ca.concordia.summs.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ParkingReservationService {

    private final ParkingGarageService parkingGarageService;
    private final UserRepository userRepository;
    private final List<PaymentStrategy> paymentStrategies;
    private final Map<String, ParkingReservation> reservationsById = new ConcurrentHashMap<>();

    public ParkingReservationService(ParkingGarageService parkingGarageService,
                                       UserRepository userRepository,
                                       List<PaymentStrategy> paymentStrategies) {
        this.parkingGarageService = parkingGarageService;
        this.userRepository = userRepository;
        this.paymentStrategies = paymentStrategies;
    }

    public long countActiveForGarage(String garageId) {
        return reservationsById.values().stream()
                .filter(r -> r.getGarageId().equals(garageId) && r.getStatus() == ParkingReservationStatus.ACTIVE)
                .count();
    }

    public int sumActiveSpotsForGarage(String garageId) {
        return reservationsById.values().stream()
                .filter(r -> r.getGarageId().equals(garageId) && r.getStatus() == ParkingReservationStatus.ACTIVE)
                .mapToInt(ParkingReservation::getSpots)
                .sum();
    }

    /** All reservations (for analytics). */
    public List<ParkingReservation> allReservations() {
        return new ArrayList<>(reservationsById.values());
    }

    public synchronized ParkingReservation reserve(String userId, String garageId, int spots,
                                                     String paymentInfo, boolean savePaymentMethod) {
        requireCitizen(userId);
        if (spots < 1) {
            throw new IllegalArgumentException("spots must be at least 1.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown user."));
        ParkingGarage garage = parkingGarageService.requireGarage(garageId);
        if (garage.getAvailableSpaces() < spots) {
            throw new IllegalStateException("Not enough available spaces at this garage.");
        }

        String effectivePayment = hasText(paymentInfo) ? paymentInfo.trim() : user.getPaymentInfo();
        if (!hasText(effectivePayment)) {
            throw new IllegalArgumentException("A payment method is required before reserving parking.");
        }

        PaymentStrategy paymentStrategy = resolvePaymentStrategy(effectivePayment);
        String paymentLabel = paymentStrategy.toPaymentLabel(effectivePayment);

        if (hasText(paymentInfo) && savePaymentMethod) {
            String label = paymentStrategy.toPaymentLabel(paymentInfo.trim());
            if (!user.getPaymentMethods().contains(label)) {
                user.getPaymentMethods().add(label);
                userRepository.save(user);
            }
        }

        double unit = Math.max(0, garage.getFlatRate());
        double amount = Math.round(unit * spots * 100.0) / 100.0;

        garage.setAvailableSpaces(garage.getAvailableSpaces() - spots);
        ParkingReservation r = new ParkingReservation(userId, garageId, spots, paymentLabel, amount);
        reservationsById.put(r.getId(), r);
        return r;
    }

    public synchronized ParkingReservation complete(String userId, String reservationId) {
        ParkingReservation r = requireOwnedActive(userId, reservationId);
        r.setStatus(ParkingReservationStatus.COMPLETED);
        r.setEndTime(LocalDateTime.now());
        releaseSpots(r);
        return r;
    }

    public synchronized ParkingReservation cancel(String userId, String reservationId) {
        ParkingReservation r = requireOwnedActive(userId, reservationId);
        r.setStatus(ParkingReservationStatus.CANCELLED);
        r.setEndTime(LocalDateTime.now());
        releaseSpots(r);
        return r;
    }

    private void releaseSpots(ParkingReservation r) {
        ParkingGarage g = parkingGarageService.requireGarage(r.getGarageId());
        g.setAvailableSpaces(Math.min(g.getTotalSpaces(), g.getAvailableSpaces() + r.getSpots()));
    }

    private ParkingReservation requireOwnedActive(String userId, String reservationId) {
        ParkingReservation r = reservationsById.get(reservationId);
        if (r == null) {
            throw new IllegalArgumentException("Reservation not found.");
        }
        if (!r.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Not your reservation.");
        }
        if (r.getStatus() != ParkingReservationStatus.ACTIVE) {
            throw new IllegalStateException("Reservation is not active.");
        }
        return r;
    }

    public List<ParkingReservation> listForUser(String userId) {
        return reservationsById.values().stream()
                .filter(r -> r.getUserId().equals(userId))
                .sorted(Comparator.comparing(ParkingReservation::getStartTime).reversed())
                .toList();
    }

    public List<Map<String, Object>> listForUserEnriched(String userId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (ParkingReservation r : listForUser(userId)) {
            parkingGarageService.getById(r.getGarageId()).ifPresent(g -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", r.getId());
                row.put("garageId", r.getGarageId());
                row.put("garageName", g.getName());
                row.put("spots", r.getSpots());
                row.put("status", r.getStatus().name());
                row.put("startTime", r.getStartTime().toString());
                row.put("endTime", r.getEndTime() != null ? r.getEndTime().toString() : "");
                row.put("paymentMethod", r.getReservationPaymentMethod());
                row.put("paymentAmount", r.getReservationPaymentAmount());
                row.put("paymentStatus", r.getReservationPaymentStatus());
                out.add(row);
            });
        }
        return out;
    }

    private void requireCitizen(String userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown user."));
        if (u.getRole() != UserRole.CITIZEN) {
            throw new IllegalArgumentException("Only citizens can reserve parking.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private PaymentStrategy resolvePaymentStrategy(String paymentInfo) {
        return paymentStrategies.stream()
                .filter(strategy -> strategy.supports(paymentInfo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported payment method."));
    }
}
