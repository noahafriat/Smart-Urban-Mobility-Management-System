package ca.concordia.summs.service;

import ca.concordia.summs.model.Rental;
import ca.concordia.summs.model.Scooter;
import ca.concordia.summs.model.User;
import ca.concordia.summs.model.UserRole;
import ca.concordia.summs.model.VehicleStatus;
import ca.concordia.summs.pattern.observer.AnalyticsObserver;
import ca.concordia.summs.repository.RentalRepository;
import ca.concordia.summs.repository.UserRepository;
import ca.concordia.summs.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AnalyticsObserver analyticsObserver;

    private RentalService rentalService;

    @BeforeEach
    void setUp() {
        rentalService = new RentalService(
                rentalRepository,
                vehicleRepository,
                userRepository,
                Collections.emptyList(),
                Collections.emptyList(),
                analyticsObserver);
    }

    @Test
    void reserveVehicle_success_marksVehicleRentedAndNotifiesObserver() {
        User user = new User("u1", "U", "u@u.ca", "pw", UserRole.CITIZEN);
        user.getPaymentMethods().add("VISA-4242");
        Scooter scooter = new Scooter("prov", "Montréal");
        String vid = scooter.getId();

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(vehicleRepository.findById(vid)).thenReturn(Optional.of(scooter));

        Rental rental = rentalService.reserveVehicle("u1", vid, null, false);

        assertNotNull(rental);
        assertEquals(VehicleStatus.RENTED, scooter.getStatus());
        verify(vehicleRepository).save(scooter);
        verify(rentalRepository).save(any(Rental.class));
        verify(analyticsObserver).onRentalCreated(eq(rental.getId()), eq(vid), eq("u1"));
    }

    @Test
    void reserveVehicle_vehicleNotAvailable_throws() {
        User user = new User("u1", "U", "u@u.ca", "pw", UserRole.CITIZEN);
        user.getPaymentMethods().add("VISA-4242");
        Scooter scooter = new Scooter("prov", "Montréal");
        scooter.setStatus(VehicleStatus.RENTED);
        String vid = scooter.getId();

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(vehicleRepository.findById(vid)).thenReturn(Optional.of(scooter));

        assertThrows(IllegalArgumentException.class, () -> rentalService.reserveVehicle("u1", vid, null, false));
    }

    @Test
    void reserveVehicle_userMissing_throws() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(
                IllegalArgumentException.class,
                () -> rentalService.reserveVehicle("missing", "v1", "VISA-1", false));
    }
}
