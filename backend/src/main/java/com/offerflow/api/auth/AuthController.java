package com.offerflow.api.auth;

import com.offerflow.api.user.User;
import com.offerflow.api.user.UserRepository;
import com.offerflow.api.user.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users; private final PasswordEncoder passwordEncoder;
    public AuthController(UserRepository users, PasswordEncoder passwordEncoder) { this.users = users; this.passwordEncoder = passwordEncoder; }
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        if (users.existsByUsername(request.username()) || users.existsByEmail(request.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Username or email is already in use"));
        }
        User user = users.save(new User(request.username(), request.email(), passwordEncoder.encode(request.password())));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", user.getId(), "username", user.getUsername()));
    }
    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal CurrentUser currentUser) {
        User user = currentUser.user();
        return Map.of("id", user.getId(), "username", user.getUsername(), "email", user.getEmail());
    }
    public record RegisterRequest(@NotBlank @Size(min = 3, max = 50) String username,
                                  @NotBlank @Email @Size(max = 120) String email,
                                  @NotBlank @Size(min = 8, max = 72) String password) { }
}
