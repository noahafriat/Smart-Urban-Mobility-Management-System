package ca.concordia.summs.service;

import ca.concordia.summs.model.*;
import ca.concordia.summs.repository.RentalRepository;
import ca.concordia.summs.repository.VehicleRepository;
import ca.concordia.summs.pattern.observer.RentalSubject;
import ca.concordia.summs.pattern.observer.AnalyticsObserver;
import ca.concordia.summs.pattern.strategy.PricingStrategy;
import ca.concordia.summs.pattern.strategy.StandardPricingStrategy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RentalService extends RentalSubject {

    private final RentalRepository rentalRepository;
    private final VehicleRepository vehicleRepository;
    
    // Automatically wires the AnalyticsObserver to this Subject
    public RentalService(RentalRepository rentalRepository, 
                         VehicleRepository vehicleRepository, 
                         AnalyticsObserver analyticsObserver) {
        this.rentalRepository = rentalRepository;
        this.vehicleRepository = vehicleRepository;
        this.addObserver(analyticsObserver); // Subject registers its Observer
    }

    public Rental reserveVehicle(String userId, String vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));
        
        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new IllegalArgumentException("This vehicle is already in use or under maintenance.");
        }
        
        // Mutate the vehicle state so others cannot reserve it
        vehicle.setStatus(VehicleStatus.IN_USE);
        vehicleRepository.save(vehicle);
        
        Rental rental = new Rental(userId, vehicle);
        rentalRepository.save(rental);
        
        // Broadcast the event to any attached Observers!
        notifyRentalCreated(rental.getId(), vehicleId, userId);
        
        return rental;
    }

    public Rental returnVehicle(String rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Rental not found."));
        
        if (rental.getStatus() != RentalStatus.ACTIVE) {
            throw new IllegalArgumentException("Rental is not currently active.");
        }
        
        // Mock a 25-minute trip for the demo, since we aren't waiting in real time
        rental.setEndTime(LocalDateTime.now().plusMinutes(25));
        rental.setStatus(RentalStatus.COMPLETED);
        
        Vehicle vehicle = rental.getVehicle();
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicleRepository.save(vehicle);
        
        // Calculate the fee dynamically using the Strategy Pattern
        PricingStrategy pricing = new StandardPricingStrategy();
        double cost = pricing.calculateCost(rental);
        rental.setTotalCost(cost);
        
        rentalRepository.save(rental);
        
        // Broadcast the event
        notifyRentalCompleted(rentalId, cost);
        
        return rental;
    }

    public Rental payForRental(String rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Rental not found."));
        
        if (rental.getStatus() != RentalStatus.COMPLETED) {
            throw new IllegalArgumentException("You must complete the trip before paying.");
        }
        
        rental.setStatus(RentalStatus.PAID);
        rentalRepository.save(rental);
        return rental;
    }
    
    public List<Rental> getUserRentals(String userId) {
        return rentalRepository.findByUserId(userId);
    }
}
