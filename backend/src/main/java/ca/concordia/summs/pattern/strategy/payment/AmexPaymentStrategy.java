package ca.concordia.summs.pattern.strategy.payment;

import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

@Component
@Order(3)
public class AmexPaymentStrategy extends AbstractCardPaymentStrategy {

    @Override
    public boolean supports(String paymentInfo) {
        if (paymentInfo == null) {
            return false;
        }
        String normalized = paymentInfo.trim().toUpperCase();
        return normalized.startsWith("AMEX") || normalized.startsWith("AMERICANEXPRESS");
    }

    @Override
    public void processPayment(String paymentInfo) {
        validateCard(paymentInfo);
    }

    @Override
    public String toPaymentLabel(String paymentInfo) {
        return suffixLabel("AMEX", paymentInfo);
    }
}
