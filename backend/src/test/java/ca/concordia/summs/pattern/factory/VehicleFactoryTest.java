package ca.concordia.summs.pattern.factory;

import ca.concordia.summs.model.Car;
import ca.concordia.summs.model.Scooter;
import ca.concordia.summs.model.Vehicle;
import ca.concordia.summs.model.VehicleType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleFactoryTest {

    @Test
    void scooterFactory_createsCorrectVehicle() {
        VehicleFactory factory = new ScooterFactory();
        Vehicle v = factory.orderVehicle("prov-1", "Montréal");
        
        assertInstanceOf(Scooter.class, v);
        assertEquals(VehicleType.SCOOTER, v.getType());
        assertEquals("prov-1", v.getProviderId());
        assertEquals("Montréal", v.getLocationCity());
        assertEquals(3.0, v.getBasePrice(), 0.001);
    }

    @Test
    void carFactory_createsCorrectVehicle_withLicensePlate() {
        VehicleFactory factory = new CarFactory();
        Vehicle v = factory.orderVehicle("prov-2", "Laval");
        
        assertInstanceOf(Car.class, v);
        assertEquals(VehicleType.CAR, v.getType());
        Car car = (Car) v;
        assertNotNull(car.getLicensePlate());
        assertTrue(car.getLicensePlate().startsWith("QC-"));
        assertEquals(20.0, v.getBasePrice(), 0.001);
    }
}
