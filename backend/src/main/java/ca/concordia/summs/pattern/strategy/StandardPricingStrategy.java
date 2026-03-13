package ca.concordia.summs.pattern.strategy;

import ca.concordia.summs.model.Rental;
import java.time.Duration;

/**
 * Concrete implementation of PricingStrategy. 
 * Fees = Base Price + (Per Minute Rate * Total Minutes)
 */
public class StandardPricingStrategy implements PricingStrategy {

    @Override
    public double calculateCost(Rental rental) {
        if (rental.getEndTime() == null) {
            return 0.0;
        }

        // Calculate minutes elapsed
        long minutes = Duration.between(rental.getStartTime(), rental.getEndTime()).toMinutes();
        
        // Ensure a minimum charge of 1 minute if the user returns it instantly
        if (minutes < 1) {
            minutes = 1;
        }

        double basePrice = rental.getVehicle().getBasePrice();
        double rate = rental.getVehicle().getPricePerMinute();

        return basePrice + (rate * minutes);
    }
}
