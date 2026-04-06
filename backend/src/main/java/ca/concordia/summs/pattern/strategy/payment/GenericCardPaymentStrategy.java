package ca.concordia.summs.pattern.strategy.payment;

import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

@Component
@Order(100)
public class GenericCardPaymentStrategy extends AbstractCardPaymentStrategy {

    @Override
    public boolean supports(String paymentInfo) {
        return paymentInfo != null && !paymentInfo.isBlank();
    }

    @Override
    public void processPayment(String paymentInfo) {
        validateCard(paymentInfo);
    }

    @Override
    public String toPaymentLabel(String paymentInfo) {
        return suffixLabel("CARD", paymentInfo);
    }
}
