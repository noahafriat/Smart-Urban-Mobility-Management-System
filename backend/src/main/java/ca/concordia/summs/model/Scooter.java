package ca.concordia.summs.model;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class Scooter extends Vehicle {
    public Scooter(String providerId, String locationCity) {
        super(providerId, VehicleType.SCOOTER, locationCity);
        this.setBasePrice(3.00);        
        this.setPricePerMinute(0.25);   
    }
}
