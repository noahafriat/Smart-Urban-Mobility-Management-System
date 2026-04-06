package ca.concordia.summs.pattern.observer;

import java.util.ArrayList;
import java.util.List;

// Subject component of the Observer Pattern.
// Inherited by the RentalService to notify listeners.

public abstract class RentalSubject {
    private final List<RentalObserver> observers = new ArrayList<>();

    public void addObserver(RentalObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(RentalObserver observer) {
        observers.remove(observer);
    }

    protected void notifyRentalCreated(String rentalId, String vehicleId, String userId) {
        for (RentalObserver obs : observers) {
            obs.onRentalCreated(rentalId, vehicleId, userId);
        }
    }

    protected void notifyRentalCompleted(String rentalId, double cost) {
        for (RentalObserver obs : observers) {
            obs.onRentalCompleted(rentalId, cost);
        }
    }
}
