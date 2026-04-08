package ca.concordia.summs.service;

import ca.concordia.summs.model.*;
import ca.concordia.summs.repository.RentalRepository;
import ca.concordia.summs.repository.UserRepository;
import ca.concordia.summs.repository.VehicleRepository;
import ca.concordia.summs.pattern.observer.RentalSubject;
import ca.concordia.summs.pattern.observer.AnalyticsObserver;
import ca.concordia.summs.pattern.strategy.payment.PaymentStrategy;
import ca.concordia.summs.pattern.strategy.pricing.PricingStrategy;
import ca.concordia.summs.pattern.strategy.pricing.StandardPricingStrategy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RentalService extends RentalSubject {

    private final RentalRepository rentalRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final List<PaymentStrategy> paymentStrategies;
    private final List<PricingStrategy> pricingStrategies;
    private final EmailService emailService;
    
    // Automatically wires the AnalyticsObserver to this Subject
    public RentalService(RentalRepository rentalRepository, 
                         VehicleRepository vehicleRepository,
                         UserRepository userRepository,
                         List<PaymentStrategy> paymentStrategies,
                         List<PricingStrategy> pricingStrategies,
                         AnalyticsObserver analyticsObserver,
                         EmailService emailService) {
        this.rentalRepository = rentalRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.paymentStrategies = paymentStrategies;
        this.pricingStrategies = pricingStrategies;
        this.emailService = emailService;
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
        
        // Send Email Confirmation
        sendReservationEmail(user, rental);
        
        return rental;
    }

    private void sendReservationEmail(User user, Rental rental) {
        Vehicle v = rental.getVehicle();
        String identifier = (v instanceof Car) ? "Plate: " + ((Car) v).getLicensePlate() : "Code: " + v.getVehicleCode();
        
        String subject = "Reservation Confirmation: " + v.getType() + " (" + (v instanceof Car ? ((Car) v).getLicensePlate() : v.getVehicleCode()) + ")";
        String body = String.format(
            "Hello %s,\n\nYour reservation for a %s is confirmed!\n\n" +
            "Reservation ID: %s\n" +
            "%s\n" +
            "Model: %s\n" +
            "Base Fee: $%.2f (Paid at reservation)\n" +
            "Payment Method: %s\n\n" +
            "Thank you for choosing SUMMS!",
            user.getName(), v.getType(),
            rental.getId(),
            identifier,
            v.getModel(),
            rental.getReservationPaymentAmount(),
            rental.getReservationPaymentMethod()
        );
        emailService.sendEmail(user.getEmail(), subject, body);
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
        PricingStrategy pricing = resolvePricingStrategy(rental);
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

        // Send Payment Receipt
        User user = userRepository.findById(rental.getUserId()).orElse(null);
        if (user != null) {
            sendReceiptEmail(user, rental);
        }

        return rental;
    }

    private void sendReceiptEmail(User user, Rental rental) {
        Vehicle v = rental.getVehicle();
        String identifier = (v instanceof Car) ? ((Car) v).getLicensePlate() : v.getVehicleCode();
        
        double baseFee = rental.getReservationPaymentAmount();
        double usageFee = Math.max(0, rental.getTotalCost() - baseFee);
        
        String subject = "Payment Receipt: Rental " + rental.getId();
        String body = String.format(
            "Hello %s,\n\nThank you for your payment! Here is your receipt:\n\n" +
            "Rental ID: %s\n" +
            "Vehicle: %s (%s)\n\n" +
            "--- Price Breakdown ---\n" +
            "Base Reservation Fee: $%.2f\n" +
            "Trip Usage Fee:      $%.2f\n" +
            "-----------------------\n" +
            "TOTAL PAID:          $%.2f\n\n" +
            "Paid With: %s\n" +
            "Date: %s\n\n" +
            "We hope you enjoyed your ride!\nSUMMS Team",
            user.getName(),
            rental.getId(),
            v.getType(), identifier,
            baseFee,
            usageFee,
            rental.getTotalCost(),
            rental.getFinalPaymentMethod(),
            rental.getFinalPaymentProcessedAt().toString()
        );
        emailService.sendEmail(user.getEmail(), subject, body);
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

    private PricingStrategy resolvePricingStrategy(Rental rental) {
        return pricingStrategies.stream()
                .filter(strategy -> strategy.supports(rental))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No pricing strategy found for this rental."));
    }
}
