package ca.concordia.summs.pattern.strategy.pricing;

import ca.concordia.summs.model.Rental;
import ca.concordia.summs.model.Scooter;
import ca.concordia.summs.model.Vehicle;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StandardPricingStrategyTest {

    private final StandardPricingStrategy strategy = new StandardPricingStrategy();

    @Test
    void calculateCost_noEndTime_returnsZero() {
        Vehicle scooter = new Scooter("p", "Montréal");
        Rental rental = new Rental("user-1", scooter, "VISA-0000", scooter.getBasePrice());
        assertEquals(0.0, strategy.calculateCost(rental), 0.001);
    }

    @Test
    void calculateCost_basePlusPerMinute() {
        Vehicle scooter = new Scooter("p", "Montréal");
        Rental rental = new Rental("user-1", scooter, "VISA-0000", scooter.getBasePrice());
        LocalDateTime start = LocalDateTime.of(2026, 3, 1, 10, 0);
        rental.setStartTime(start);
        rental.setEndTime(start.plusMinutes(10));
        // base 3.0 + 0.25 * 10 = 5.5
        assertEquals(5.5, strategy.calculateCost(rental), 0.001);
    }

    @Test
    void calculateCost_subMinuteRoundedUpToOneMinute() {
        Vehicle scooter = new Scooter("p", "Montréal");
        Rental rental = new Rental("user-1", scooter, "VISA-0000", scooter.getBasePrice());
        LocalDateTime start = LocalDateTime.of(2026, 3, 1, 10, 0);
        rental.setStartTime(start);
        rental.setEndTime(start.plusSeconds(30));
        // minutes < 1 -> 1 minute: 3.0 + 0.25
        assertEquals(3.25, strategy.calculateCost(rental), 0.001);
    }
}
