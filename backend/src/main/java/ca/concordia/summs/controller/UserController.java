package ca.concordia.summs.controller;
import ca.concordia.summs.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // POST /api/users/register
    // Create a new user account
    @PostMapping("/register")
    public ResponseEntity<Object> register(@RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(userService.register(
                    body.get("name"),
                    body.get("email"),
                    body.get("password"),
                    body.getOrDefault("role", "CITIZEN"),
                    body.get("phone")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/users/login
    // Authenticate a user and return user data
    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody Map<String, String> body) {
        String email    = body.get("email");
        String password = body.get("password");
        return userService.getRepository().findByEmail(email)
                .filter(u -> u.getPassword().equals(password))
                .map(u -> ResponseEntity.ok((Object) userService.toResponse(u)))
                .orElse(ResponseEntity.status(401).body(Map.of("error", "Invalid email or password.")));
    }

    // PUT /api/users/{id}/profile
    // Update a user's profile
    @PutMapping("/{id}/profile")
    public ResponseEntity<Object> updateProfile(@PathVariable String id,
                                                 @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(userService.updateProfile(id, body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/users/{id}/role
    // Change a user's role (admin only)
    @PutMapping("/{id}/role")
    public ResponseEntity<Object> changeRole(@PathVariable String id,
                                              @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(userService.changeRole(id, body.get("role")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/users
    // List all users
    @GetMapping
    public ResponseEntity<Object> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
}
