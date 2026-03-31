package ca.concordia.summs.pattern.strategy;

/**
 * Strategy pattern interface for simulated payment processing.
 * Each strategy decides whether it supports a given payment label
 * and applies its own validation/processing rules.
 */
public interface PaymentStrategy {
    boolean supports(String paymentInfo);

    void processPayment(String paymentInfo);

    String toPaymentLabel(String paymentInfo);
}
