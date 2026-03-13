package ca.concordia.summs.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Car extends Vehicle {
    private String licensePlate;

    public Car(String providerId, String locationCity, String licensePlate) {
        super(providerId, VehicleType.CAR, locationCity);
        this.setBasePrice(20.00);        
        this.setPricePerMinute(2.00);   
        this.licensePlate = licensePlate;
    }
}
