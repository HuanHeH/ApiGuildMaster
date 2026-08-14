package dam.guildmaster.controller;

import dam.guildmaster.entity.User;
import dam.guildmaster.dto.LoginRequest;
import dam.guildmaster.security.AccessService;
import dam.guildmaster.security.AuthUser;
import dam.guildmaster.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AccessService accessService;

    public UserController(UserService userService, AccessService accessService) {
        this.userService = userService;
        this.accessService = accessService;
    }

    @GetMapping
    public ResponseEntity<?> getAll(@RequestParam(value = "guild_id", required = false) Integer guildId) {
        AuthUser me = accessService.requireUser();
        return userService.findAll(me, guildId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id,
                                     @RequestParam(value = "guild_id", required = false) Integer guildId) {
        AuthUser me = accessService.requireUser();
        return userService.findById(me, id, guildId);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody User user) {
        accessService.requireAdmin();
        return userService.create(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody User data) {
        AuthUser me = accessService.requireUser();
        return userService.update(me, id, data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        accessService.requireAdmin();
        return userService.delete(id);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return userService.login(request);
    }
}
