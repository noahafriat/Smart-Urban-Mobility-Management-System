package ca.concordia.summs.pattern.strategy.payment;

/**
 * Shared fake-card validation for brand-specific payment strategies.
 */
public abstract class AbstractCardPaymentStrategy implements PaymentStrategy {

    protected void validateCard(String paymentInfo) {
        String digitsOnly = paymentInfo.replaceAll("\\D", "");
        if (digitsOnly.length() > 0 && digitsOnly.length() < 4) {
            throw new IllegalArgumentException("Enter a valid fake credit card.");
        }
        if (digitsOnly.endsWith("0000")) {
            throw new IllegalArgumentException("Payment was declined. Try a different payment method.");
        }
    }

    protected String suffixLabel(String prefix, String paymentInfo) {
        String trimmed = paymentInfo.trim();
        if (trimmed.matches(".*-\\d{4}$")) {
            return trimmed.toUpperCase();
        }

        String digitsOnly = trimmed.replaceAll("\\D", "");
        if (digitsOnly.length() >= 4) {
            return prefix + "-" + digitsOnly.substring(digitsOnly.length() - 4);
        }
        return prefix;
    }
}
