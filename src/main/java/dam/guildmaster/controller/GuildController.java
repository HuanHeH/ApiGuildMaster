package dam.guildmaster.controller;

import dam.guildmaster.entity.Guild;
import dam.guildmaster.security.AccessService;
import dam.guildmaster.security.AuthUser;
import dam.guildmaster.service.GuildService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/guilds")
public class GuildController {

    private final GuildService guildService;
    private final AccessService accessService;

    public GuildController(GuildService guildService, AccessService accessService) {
        this.guildService = guildService;
        this.accessService = accessService;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        AuthUser me = accessService.requireUser();
        return ResponseEntity.ok(guildService.findAll(me));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        return guildService.findById(id);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Guild guild) {
        accessService.requireAdmin();
        return guildService.create(guild);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody Guild data) {
        accessService.requireAdmin();
        return guildService.update(id, data);
    }

    @PutMapping("/{id}/name")
    public ResponseEntity<?> updateName(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        AuthUser me = accessService.requireUser();
        String name = body == null ? null : body.get("name");
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "name is required"));
        }
        if (name.length() > 100) {
            return ResponseEntity.badRequest().body(Map.of("message", "name must be 100 characters or fewer"));
        }
        if (!me.isAdmin()) {
            accessService.requireTeacherGuild(id);
        }
        return guildService.updateName(id, name.trim());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        accessService.requireAdmin();
        return guildService.delete(id);
    }
}
