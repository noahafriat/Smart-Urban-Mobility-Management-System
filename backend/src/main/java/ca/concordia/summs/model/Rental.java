package ca.concordia.summs.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a vehicle rental transaction.
 */
@Data
public class Rental {
    private final String id;
    private final String userId;
    private final Vehicle vehicle;
    private RentalStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double totalCost;
    private String reservationPaymentMethod;
    private String reservationPaymentStatus;
    private LocalDateTime reservationPaymentProcessedAt;
    private double reservationPaymentAmount;

    public Rental(String userId, Vehicle vehicle, String reservationPaymentMethod, double reservationPaymentAmount) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.vehicle = vehicle;
        this.status = RentalStatus.ACTIVE;
        this.startTime = LocalDateTime.now();
        this.reservationPaymentMethod = reservationPaymentMethod;
        this.reservationPaymentStatus = "SUCCEEDED";
        this.reservationPaymentProcessedAt = LocalDateTime.now();
        this.reservationPaymentAmount = reservationPaymentAmount;
    }
}
