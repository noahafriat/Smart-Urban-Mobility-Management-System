package ca.concordia.summs.pattern.strategy;

import ca.concordia.summs.model.Rental;

/**
 * Strategy pattern interface for decoupling the pricing calculation
 * from the RentalService.
 */
public interface PricingStrategy {
    boolean supports(Rental rental);
    double calculateCost(Rental rental);
}
