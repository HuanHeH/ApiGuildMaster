package dam.guildmaster.service;

import dam.guildmaster.entity.Guild;
import dam.guildmaster.repository.GuildRepository;
import dam.guildmaster.security.AccessService;
import dam.guildmaster.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class GuildService {

    private final GuildRepository guildRepository;
    private final AccessService accessService;

    public GuildService(GuildRepository guildRepository, AccessService accessService) {
        this.guildRepository = guildRepository;
        this.accessService = accessService;
    }

    public List<Guild> findAll(AuthUser me) {
        if (me.isAdmin()) {
            return guildRepository.findAll();
        }
        List<Integer> ids = accessService.scopedGuildIds(me);
        return guildRepository.findByIdIn(ids);
    }

    public ResponseEntity<Guild> findById(Integer id) {
        accessService.assertCanAccessGuild(id);
        return guildRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<?> create(Guild guild) {
        String err = validateRequired(guild);
        if (err != null) {
            return ResponseEntity.badRequest().body(Map.of("message", err));
        }
        guild.setId(null);
        normalizeOptionals(guild);
        return ResponseEntity.status(HttpStatus.CREATED).body(guildRepository.save(guild));
    }

    public ResponseEntity<?> update(Integer id, Guild data) {
        return guildRepository.findById(id).<ResponseEntity<?>>map(guild -> {
            if (data.getName() != null) guild.setName(data.getName());
            if (data.getNumber() != null) guild.setNumber(data.getNumber());
            if (data.getLetter() != null) guild.setLetter(data.getLetter());
            guild.setLevel(blankToNull(data.getLevel()));
            guild.setModality(blankToNull(data.getModality()));
            String err = validateRequired(guild);
            if (err != null) {
                return ResponseEntity.badRequest().body(Map.of("message", err));
            }
            return ResponseEntity.ok(guildRepository.save(guild));
        }).orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<?> delete(Integer id) {
        if (!guildRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        guildRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<?> updateName(Integer id, String name) {
        return guildRepository.findById(id).<ResponseEntity<?>>map(guild -> {
            guild.setName(name);
            return ResponseEntity.ok(guildRepository.save(guild));
        }).orElse(ResponseEntity.notFound().build());
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
