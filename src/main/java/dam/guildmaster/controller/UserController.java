package dam.guildmaster.controller;

import dam.guildmaster.dto.LoginRequest;
import dam.guildmaster.dto.LoginResponse;
import dam.guildmaster.entity.User;
import dam.guildmaster.enums.Role;
import dam.guildmaster.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<User> getAll() {
        return userRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getById(@PathVariable Integer id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody User user) {
        if (user.getMail() == null || user.getMail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "mail is required"));
        }
        if (userRepository.findByMailIgnoreCase(user.getMail().trim()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "mail already exists"));
        }
        user.setId(null);
        user.setMail(user.getMail().trim());
        if (user.getHash() != null && !user.getHash().startsWith("$2")) {
            user.setHash(passwordEncoder.encode(user.getHash()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(userRepository.save(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody User data) {
        return userRepository.findById(id).<ResponseEntity<?>>map(user -> {
            if (data.getMail() != null) {
                String mail = data.getMail().trim();
                if (mail.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("message", "mail is required"));
                }
                var existing = userRepository.findByMailIgnoreCase(mail);
                if (existing.isPresent() && !existing.get().getId().equals(id)) {
                    return ResponseEntity.badRequest().body(Map.of("message", "mail already exists"));
                }
                user.setMail(mail);
            }
            if (data.getName() != null) user.setName(data.getName());
            if (data.getHash() != null) {
                if (!data.getHash().startsWith("$2")) {
                    user.setHash(passwordEncoder.encode(data.getHash()));
                } else {
                    user.setHash(data.getHash());
                }
            }
            if (data.getRole() != null) user.setRole(data.getRole());
            return ResponseEntity.ok(userRepository.save(user));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.getMail() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "mail and password are required"));
        }

        return userRepository.findByMailIgnoreCase(request.getMail())
                .filter(user -> matchesPassword(request.getPassword(), user.getHash()))
                .filter(user -> user.getRole() == Role.Admin)
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(
                        new LoginResponse(user.getId(), user.getName(), user.getMail(), user.getRole())
                ))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid credentials or user is not Admin")));
    }

    private boolean matchesPassword(String rawPassword, String storedHash) {
        if (storedHash == null || rawPassword == null) {
            return false;
        }
        if (storedHash.startsWith("$2")) {
            return passwordEncoder.matches(rawPassword, storedHash);
        }
        // Compatibility with plain-text hashes from the initial DB seed
        return storedHash.equals(rawPassword);
    }
}
