package ca.concordia.summs.pattern.observer;

import ca.concordia.summs.pattern.singleton.AnalyticsEngine;
import org.springframework.stereotype.Component;

/**
 * Concrete Observer that pushes rental events to the Singleton AnalyticsEngine.
 */
@Component
public class AnalyticsObserver implements RentalObserver {

    @Override
    public void onRentalCreated(String rentalId, String vehicleId, String userId) {
        AnalyticsEngine.getInstance().logEvent(
                "RENTAL_CREATED", 
                "Rental " + rentalId + " started by " + userId + " for vehicle " + vehicleId
        );
    }

    @Override
    public void onRentalCompleted(String rentalId, double cost) {
        AnalyticsEngine.getInstance().logEvent(
                "RENTAL_COMPLETED", 
                "Rental " + rentalId + " finished. Cost: $" + cost
        );
    }
}
