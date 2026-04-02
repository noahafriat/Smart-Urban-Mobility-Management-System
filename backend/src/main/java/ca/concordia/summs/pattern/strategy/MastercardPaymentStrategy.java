package ca.concordia.summs.pattern.strategy;

import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

@Component
@Order(2)
public class MastercardPaymentStrategy extends AbstractCardPaymentStrategy {

    @Override
    public boolean supports(String paymentInfo) {
        if (paymentInfo == null) {
            return false;
        }
        String normalized = paymentInfo.trim().toUpperCase();
        return normalized.startsWith("MASTERCARD") || normalized.startsWith("MC");
    }

    @Override
    public void processPayment(String paymentInfo) {
        validateCard(paymentInfo);
    }

    @Override
    public String toPaymentLabel(String paymentInfo) {
        return suffixLabel("MASTERCARD", paymentInfo);
    }
}
