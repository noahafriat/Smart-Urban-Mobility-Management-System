package ca.concordia.summs.pattern.strategy.pricing;

import ca.concordia.summs.model.Rental;
import ca.concordia.summs.model.VehicleType;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import java.time.Duration;

@Component
@Order(10)
public class ScooterPricingStrategy implements PricingStrategy {

    @Override
    public boolean supports(Rental rental) {
        return rental.getVehicle() != null && rental.getVehicle().getType() == VehicleType.SCOOTER;
    }

    @Override
    public double calculateCost(Rental rental) {
        if (rental.getEndTime() == null) return 0.0;
        long minutes = Math.max(1, Duration.between(rental.getStartTime(), rental.getEndTime()).toMinutes());
        
        double basePrice = rental.getVehicle().getBasePrice();
        double rate = rental.getVehicle().getPricePerMinute();
        
        // Specific rule for scooters: halving the unlock fee
        return (basePrice * 0.5) + (rate * minutes);
    }
}
