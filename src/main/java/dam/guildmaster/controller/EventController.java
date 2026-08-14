package dam.guildmaster.controller;

import dam.guildmaster.entity.GameEvent;
import dam.guildmaster.security.AccessService;
import dam.guildmaster.security.AuthUser;
import dam.guildmaster.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final AccessService accessService;

    public EventController(EventService eventService, AccessService accessService) {
        this.eventService = eventService;
        this.accessService = accessService;
    }

    @GetMapping
    public ResponseEntity<?> getAll(@RequestParam(value = "guild_id", required = false) Integer guildId) {
        AuthUser me = accessService.requireUser();
        return ResponseEntity.ok(eventService.findAll(me, guildId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id,
                                     @RequestParam(value = "guild_id", required = false) Integer guildId) {
        AuthUser me = accessService.requireUser();
        return eventService.findById(me, id, guildId);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody GameEvent event) {
        AuthUser me = accessService.requireUser();
        return eventService.create(me, event);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody GameEvent data) {
        AuthUser me = accessService.requireUser();
        return eventService.update(me, id, data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        accessService.requireAdmin();
        return eventService.delete(id);
    }
}
