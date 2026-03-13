package ca.concordia.summs.model;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class Bike extends Vehicle {
    public Bike(String providerId, String locationCity) {
        super(providerId, VehicleType.BIKE, locationCity);
        this.setBasePrice(5.00);        
        this.setPricePerMinute(0.25);   
    }
}
