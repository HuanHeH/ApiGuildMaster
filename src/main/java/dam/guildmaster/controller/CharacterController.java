package dam.guildmaster.controller;

import dam.guildmaster.entity.GameCharacter;
import dam.guildmaster.entity.Skill;
import dam.guildmaster.security.AccessService;
import dam.guildmaster.security.AuthUser;
import dam.guildmaster.service.CharacterService;
import dam.guildmaster.service.SkillService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/characters")
public class CharacterController {

    private final CharacterService characterService;
    private final AccessService accessService;

    public CharacterController(CharacterService characterService, AccessService accessService) {
        this.characterService = characterService;
        this.accessService = accessService;
    }

    @GetMapping
    public ResponseEntity<?> getAll(@RequestParam(value = "guild_id", required = false) Integer guildId) {
        AuthUser me = accessService.requireUser();
        return ResponseEntity.ok(characterService.findAll(me, guildId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id,
                                     @RequestParam(value = "guild_id", required = false) Integer guildId) {
        AuthUser me = accessService.requireUser();
        return characterService.findById(me, id, guildId);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody GameCharacter character) {
        AuthUser me = accessService.requireUser();
        return characterService.create(me, character);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody Map<String, Object> data) {
        AuthUser me = accessService.requireUser();
        return characterService.update(me, id, data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        AuthUser me = accessService.requireUser();
        return characterService.delete(me, id);
    }
}
