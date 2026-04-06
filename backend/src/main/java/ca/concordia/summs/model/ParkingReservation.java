package ca.concordia.summs.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class ParkingReservation {
    private final String id;
    private final String userId;
    private final String garageId;
    private final int spots;
    private ParkingReservationStatus status;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private final String reservationPaymentMethod;
    private final String reservationPaymentStatus;
    private final LocalDateTime reservationPaymentProcessedAt;
    private final double reservationPaymentAmount;

    public ParkingReservation(String userId, String garageId, int spots,
            String reservationPaymentMethod, double reservationPaymentAmount) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.garageId = garageId;
        this.spots = spots;
        this.status = ParkingReservationStatus.ACTIVE;
        this.startTime = LocalDateTime.now();
        this.reservationPaymentMethod = reservationPaymentMethod;
        this.reservationPaymentStatus = "SUCCEEDED";
        this.reservationPaymentProcessedAt = LocalDateTime.now();
        this.reservationPaymentAmount = Math.round(reservationPaymentAmount * 100.0) / 100.0;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getGarageId() {
        return garageId;
    }

    public int getSpots() {
        return spots;
    }

    public ParkingReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ParkingReservationStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getReservationPaymentMethod() {
        return reservationPaymentMethod;
    }

    public String getReservationPaymentStatus() {
        return reservationPaymentStatus;
    }

    public LocalDateTime getReservationPaymentProcessedAt() {
        return reservationPaymentProcessedAt;
    }

    public double getReservationPaymentAmount() {
        return reservationPaymentAmount;
    }
}
