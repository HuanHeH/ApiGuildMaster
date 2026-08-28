package dam.guildmaster.controller;

import dam.guildmaster.entity.Party;
import dam.guildmaster.security.AccessService;
import dam.guildmaster.security.AuthUser;
import dam.guildmaster.service.PartyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parties")
public class PartyController {

    private final PartyService partyService;
    private final AccessService accessService;

    public PartyController(PartyService partyService, AccessService accessService) {
        this.partyService = partyService;
        this.accessService = accessService;
    }

    @GetMapping
    public ResponseEntity<?> getAll(@RequestParam(value = "guild_id", required = false) Integer guildId) {
        AuthUser me = accessService.requireUser();
        return ResponseEntity.ok(partyService.findAll(me, guildId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id,
                                     @RequestParam(value = "guild_id", required = false) Integer guildId) {
        AuthUser me = accessService.requireUser();
        return partyService.findById(me, id, guildId);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Party party) {
        accessService.requireAdminOrTeacher();
        AuthUser me = accessService.requireUser();
        return partyService.create(me, party);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody Party data) {
        accessService.requireAdminOrTeacher();
        AuthUser me = accessService.requireUser();
        return partyService.update(me, id, data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        accessService.requireAdminOrTeacher();
        AuthUser me = accessService.requireUser();
        return partyService.delete(me, id);
    }
}
