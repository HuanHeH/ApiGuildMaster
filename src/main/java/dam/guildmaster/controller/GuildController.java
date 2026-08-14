package dam.guildmaster.controller;

import dam.guildmaster.entity.Guild;
import dam.guildmaster.security.AccessService;
import dam.guildmaster.security.AuthUser;
import dam.guildmaster.service.GuildService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        accessService.requireAdmin();
        return guildService.delete(id);
    }
}
