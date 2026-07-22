package dam.guildmaster.controller;

import dam.guildmaster.entity.GameCharacter;
import dam.guildmaster.repository.CharacterRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/characters")
public class CharacterController {

    private final CharacterRepository characterRepository;

    public CharacterController(CharacterRepository characterRepository) {
        this.characterRepository = characterRepository;
    }

    @GetMapping
    public List<GameCharacter> getAll() {
        return characterRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameCharacter> getById(@PathVariable Integer id) {
        return characterRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<GameCharacter> create(@RequestBody GameCharacter character) {
        character.setId(null);
        // Party is optional
        if (character.getPartyId() != null && character.getPartyId() <= 0) {
            character.setPartyId(null);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(characterRepository.save(character));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody Map<String, Object> data) {
        return characterRepository.findById(id).<ResponseEntity<?>>map(character -> {
            if (data.containsKey("name") && data.get("name") != null) {
                character.setName(String.valueOf(data.get("name")));
            }
            if (data.containsKey("job") && data.get("job") != null) {
                character.setJob(String.valueOf(data.get("job")));
            }
            if (data.containsKey("level") && data.get("level") != null) {
                character.setLevel(((Number) data.get("level")).intValue());
            }
            if (data.containsKey("exp") && data.get("exp") != null) {
                character.setExp(((Number) data.get("exp")).intValue());
            }
            if (data.containsKey("user_id") && data.get("user_id") != null) {
                character.setUserId(((Number) data.get("user_id")).intValue());
            }
            if (data.containsKey("guild_id") && data.get("guild_id") != null) {
                character.setGuildId(((Number) data.get("guild_id")).intValue());
            }
            // Party optional: key present with null → clear FK to NULL
            if (data.containsKey("party_id")) {
                Object party = data.get("party_id");
                if (party == null || "".equals(String.valueOf(party).trim())) {
                    character.setPartyId(null);
                } else {
                    character.setPartyId(((Number) party).intValue());
                }
            }
            return ResponseEntity.ok(characterRepository.save(character));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!characterRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        characterRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
