package dam.guildmaster.controller;

import dam.guildmaster.entity.Guild;
import dam.guildmaster.repository.GuildRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/guilds")
public class GuildController {

    private final GuildRepository guildRepository;

    public GuildController(GuildRepository guildRepository) {
        this.guildRepository = guildRepository;
    }

    @GetMapping
    public List<Guild> getAll() {
        return guildRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Guild> getById(@PathVariable Integer id) {
        return guildRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Guild guild) {
        String err = validateRequired(guild);
        if (err != null) {
            return ResponseEntity.badRequest().body(Map.of("message", err));
        }
        guild.setId(null);
        normalizeOptionals(guild);
        return ResponseEntity.status(HttpStatus.CREATED).body(guildRepository.save(guild));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody Guild data) {
        return guildRepository.findById(id).<ResponseEntity<?>>map(guild -> {
            if (data.getName() != null) guild.setName(data.getName());
            if (data.getNumber() != null) guild.setNumber(data.getNumber());
            if (data.getLetter() != null) guild.setLetter(data.getLetter());
            // Level / Modality optional → may be set to NULL
            guild.setLevel(blankToNull(data.getLevel()));
            guild.setModality(blankToNull(data.getModality()));

            String err = validateRequired(guild);
            if (err != null) {
                return ResponseEntity.badRequest().body(Map.of("message", err));
            }
            return ResponseEntity.ok(guildRepository.save(guild));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!guildRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        guildRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private static void normalizeOptionals(Guild guild) {
        guild.setLevel(blankToNull(guild.getLevel()));
        guild.setModality(blankToNull(guild.getModality()));
        if (guild.getLetter() != null) {
            guild.setLetter(guild.getLetter().trim());
        }
    }

    private static String validateRequired(Guild guild) {
        if (guild.getName() == null || guild.getName().isBlank()) return "name is required";
        if (guild.getNumber() == null) return "number is required";
        if (guild.getLetter() == null || guild.getLetter().isBlank()) return "letter is required";
        return null;
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
