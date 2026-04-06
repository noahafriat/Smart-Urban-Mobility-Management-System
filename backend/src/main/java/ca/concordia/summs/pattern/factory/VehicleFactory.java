package ca.concordia.summs.pattern.factory;
import ca.concordia.summs.model.*;

public abstract class VehicleFactory {

    public Vehicle orderVehicle(String providerId, String locationCity) {
        Vehicle vehicle = createVehicle(providerId, locationCity);
        return vehicle;
    }
    
    protected abstract Vehicle createVehicle(String providerId, String locationCity);
}