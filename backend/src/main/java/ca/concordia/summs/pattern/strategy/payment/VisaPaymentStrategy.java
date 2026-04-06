package ca.concordia.summs.pattern.strategy.payment;

import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

@Component
@Order(1)
public class VisaPaymentStrategy extends AbstractCardPaymentStrategy {

    @Override
    public boolean supports(String paymentInfo) {
        return paymentInfo != null && paymentInfo.trim().toUpperCase().startsWith("VISA");
    }

    @Override
    public void processPayment(String paymentInfo) {
        validateCard(paymentInfo);
    }

    @Override
    public String toPaymentLabel(String paymentInfo) {
        return suffixLabel("VISA", paymentInfo);
    }
}
