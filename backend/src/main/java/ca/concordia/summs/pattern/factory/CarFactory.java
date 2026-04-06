package ca.concordia.summs.pattern.factory;
import ca.concordia.summs.model.*;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component("carFactory")
public class CarFactory extends VehicleFactory {
    @Override
    protected Vehicle createVehicle(String providerId, String locationCity) {
        // Generate a random mock license plate for cars
        String license = "QC-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        return new Car(providerId, locationCity, license);
    }
}