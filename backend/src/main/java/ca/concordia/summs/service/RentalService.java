package ca.concordia.summs.service;

import ca.concordia.summs.model.*;
import ca.concordia.summs.repository.RentalRepository;
import ca.concordia.summs.repository.UserRepository;
import ca.concordia.summs.repository.VehicleRepository;
import ca.concordia.summs.pattern.observer.RentalSubject;
import ca.concordia.summs.pattern.observer.AnalyticsObserver;
import ca.concordia.summs.pattern.strategy.PaymentStrategy;
import ca.concordia.summs.pattern.strategy.PricingStrategy;
import ca.concordia.summs.pattern.strategy.StandardPricingStrategy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RentalService extends RentalSubject {

    private final RentalRepository rentalRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final List<PaymentStrategy> paymentStrategies;
    
    // Automatically wires the AnalyticsObserver to this Subject
    public RentalService(RentalRepository rentalRepository, 
                         VehicleRepository vehicleRepository,
                         UserRepository userRepository,
                         List<PaymentStrategy> paymentStrategies,
                         AnalyticsObserver analyticsObserver) {
        this.rentalRepository = rentalRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.paymentStrategies = paymentStrategies;
        this.addObserver(analyticsObserver); // Subject registers its Observer
    }

    public Rental reserveVehicle(String userId, String vehicleId, String paymentInfo, boolean savePaymentMethod) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));

        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new IllegalArgumentException("This vehicle is already in use or under maintenance.");
        }

        String effectivePaymentInfo = hasText(paymentInfo) ? paymentInfo.trim() : user.getPaymentInfo();
        if (!hasText(effectivePaymentInfo)) {
            throw new IllegalArgumentException("A payment method is required before reserving.");
        }
        
        PaymentStrategy paymentStrategy = resolvePaymentStrategy(effectivePaymentInfo);
        String paymentLabel = paymentStrategy.toPaymentLabel(effectivePaymentInfo);
        
        // Add to user's saved methods if it's new and they want to save it
        if (hasText(paymentInfo) && savePaymentMethod) {
            String label = paymentStrategy.toPaymentLabel(paymentInfo);
            if (!user.getPaymentMethods().contains(label)) {
                user.getPaymentMethods().add(label);
                userRepository.save(user);
            }
        }
        
        // Mutate the vehicle state so others cannot reserve it
        vehicle.setStatus(VehicleStatus.RENTED);
        vehicleRepository.save(vehicle);
        
        Rental rental = new Rental(userId, vehicle, paymentLabel, vehicle.getBasePrice());
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
        
        // NOW WE CHARGE - Using the card saved at the start
        String method = rental.getReservationPaymentMethod();
        resolvePaymentStrategy(method).processPayment(method);
        
        rental.setFinalPaymentMethod(method);
        rental.setFinalPaymentProcessedAt(LocalDateTime.now());
        rental.setStatus(RentalStatus.PAID);
        rentalRepository.save(rental);
        return rental;
    }
    
    public List<Rental> getUserRentals(String userId) {
        return rentalRepository.findByUserId(userId);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private PaymentStrategy resolvePaymentStrategy(String paymentInfo) {
        return paymentStrategies.stream()
                .filter(strategy -> strategy.supports(paymentInfo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported payment method."));
    }
}
