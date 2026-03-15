package ca.concordia.summs.service;
import ca.concordia.summs.model.User;
import ca.concordia.summs.model.UserRole;
import ca.concordia.summs.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.*;

 // This service layer covers: registration, profile update, role management, and user listing.
 
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // register new user
    public Map<String, Object> register(String name, String email, String password,
                                        String roleStr, String phone, String preferredMobilityType) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        UserRole role;
        try {
            role = UserRole.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            role = UserRole.CITIZEN; // safe default
        }

        User user = new User(name, email, password, role);
        if (phone != null && !phone.isBlank()) user.setPhone(phone);
        if (preferredMobilityType != null && !preferredMobilityType.isBlank()) {
            user.setPreferredMobilityType(preferredMobilityType);
        }
        userRepository.save(user);

        return toResponse(user);
    }


    // Update mutable profile fields for an existing user
    public Map<String, Object> updateProfile(String userId, Map<String, String> updates) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (updates.containsKey("name"))                 user.setName(updates.get("name"));
        if (updates.containsKey("phone"))                user.setPhone(updates.get("phone"));
        if (updates.containsKey("preferredCity"))        user.setPreferredCity(updates.get("preferredCity"));
        if (updates.containsKey("preferredMobilityType"))user.setPreferredMobilityType(updates.get("preferredMobilityType"));
        if (updates.containsKey("paymentInfo"))          user.setPaymentInfo(updates.get("paymentInfo"));
        if (updates.containsKey("password") && !updates.get("password").isBlank())
            user.setPassword(updates.get("password"));

        userRepository.save(user);
        return toResponse(user);
    }

    // Change a user's role (System Admin only. It is enforced at the controller level)
    public Map<String, Object> changeRole(String targetUserId, String newRole) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        user.setRole(UserRole.valueOf(newRole.toUpperCase()));
        userRepository.save(user);
        return toResponse(user);
    }

    public void deleteUser(String requesterId, String targetUserId) {
        if (requesterId.equals(targetUserId)) {
            throw new IllegalArgumentException("You cannot delete your own account.");
        }
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        // Safety: prevent deleting the last System Admin
        if (target.getRole() == UserRole.SYSTEM_ADMIN) {
            long sysAdminCount = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == UserRole.SYSTEM_ADMIN).count();
            if (sysAdminCount <= 1) {
                throw new IllegalArgumentException("Cannot delete the last System Admin.");
            }
        }
        userRepository.deleteById(targetUserId);
    }

    public List<Map<String, Object>> getAllUsers() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    public UserRepository getRepository() { return userRepository; }

    // Helper methods 

    public Map<String, Object> toResponse(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id",                   user.getId());
        map.put("name",                 user.getName());
        map.put("email",                user.getEmail());
        map.put("role",                 user.getRole());
        map.put("phone",                user.getPhone());
        map.put("preferredCity",        user.getPreferredCity());
        map.put("preferredMobilityType",user.getPreferredMobilityType());
        map.put("paymentInfo",          user.getPaymentInfo());
        return map;
    }
}
