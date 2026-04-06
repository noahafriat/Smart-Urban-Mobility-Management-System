package ca.concordia.summs.pattern.factory;
import ca.concordia.summs.model.*;
import org.springframework.stereotype.Component;

@Component("scooterFactory")
public class ScooterFactory extends VehicleFactory {
    @Override
    protected Vehicle createVehicle(String provider, String city) {
        return new Scooter(provider, city);
    }
}