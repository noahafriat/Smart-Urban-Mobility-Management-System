package ca.concordia.summs.pattern.observer;

public interface RentalObserver {
    void onRentalCreated(String rentalId, String vehicleId, String userId);
    void onRentalCompleted(String rentalId, double cost);
}
