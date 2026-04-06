package ca.concordia.summs.pattern.observer;

// Observer interface

public interface RentalObserver {
    void onRentalCreated(String rentalId, String vehicleId, String userId);
    void onRentalCompleted(String rentalId, double cost);
}
