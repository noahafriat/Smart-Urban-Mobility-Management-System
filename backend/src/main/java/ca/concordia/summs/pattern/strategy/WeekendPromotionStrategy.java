package ca.concordia.summs.pattern.strategy;

import ca.concordia.summs.model.Rental;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import java.time.DayOfWeek;
import java.time.Duration;

@Component
@Order(1) // Highest priority
public class WeekendPromotionStrategy implements PricingStrategy {

    @Override
    public boolean supports(Rental rental) {
        if (rental.getEndTime() == null) return false;
        DayOfWeek day = rental.getEndTime().getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    @Override
    public double calculateCost(Rental rental) {
        if (rental.getEndTime() == null) return 0.0;
        long minutes = Math.max(1, Duration.between(rental.getStartTime(), rental.getEndTime()).toMinutes());
        
        double basePrice = rental.getVehicle().getBasePrice();
        double rate = rental.getVehicle().getPricePerMinute();
        
        double standardCost = basePrice + (rate * minutes);
        return standardCost * 0.8; // 20% discount on weekends
    }
}
