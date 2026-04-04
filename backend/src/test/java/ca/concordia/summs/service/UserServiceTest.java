package ca.concordia.summs.service;

import ca.concordia.summs.model.User;
import ca.concordia.summs.model.UserRole;
import ca.concordia.summs.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void register_duplicateEmail_throws() {
        when(userRepository.existsByEmail("taken@x.ca")).thenReturn(true);
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register("N", "taken@x.ca", "pw", "CITIZEN", null, null));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_invalidRole_defaultsToCitizen() {
        when(userRepository.existsByEmail("new@x.ca")).thenReturn(false);
        userService.register("N", "new@x.ca", "pw", "NOT_A_REAL_ROLE", null, null);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(UserRole.CITIZEN, captor.getValue().getRole());
    }

    @Test
    void updateProfile_splitsPaymentMethods() {
        User u = new User("id-1", "Name", "e@e.ca", "pw", UserRole.CITIZEN);
        when(userRepository.findById("id-1")).thenReturn(Optional.of(u));

        userService.updateProfile("id-1", Map.of("paymentMethods", " A , B , "));

        assertEquals(List.of("A", "B"), u.getPaymentMethods());
        verify(userRepository).save(u);
    }

    @Test
    void deleteUser_cannotDeleteSelf() {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.deleteUser("same", "same"));
        verify(userRepository, never()).deleteById(any());
    }
}
