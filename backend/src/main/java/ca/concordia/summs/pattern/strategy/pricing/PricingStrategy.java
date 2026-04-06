package ca.concordia.summs.pattern.strategy.pricing;

import ca.concordia.summs.model.Rental;

// Strategy design pattern interface for decoupling the pricing calculation
// from the RentalService.

public interface PricingStrategy {
    boolean supports(Rental rental);
    double calculateCost(Rental rental);
}
