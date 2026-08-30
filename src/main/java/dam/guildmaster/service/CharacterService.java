package dam.guildmaster.service;

import dam.guildmaster.entity.GameCharacter;
import dam.guildmaster.entity.User;
import dam.guildmaster.enums.Role;
import dam.guildmaster.repository.CharacterRepository;
import dam.guildmaster.repository.PartyRepository;
import dam.guildmaster.repository.UserRepository;
import dam.guildmaster.security.AccessService;
import dam.guildmaster.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CharacterService {

    private final CharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final PartyRepository partyRepository;
    private final AccessService accessService;

    public CharacterService(
            CharacterRepository characterRepository,
            UserRepository userRepository,
            PartyRepository partyRepository,
            AccessService accessService) {
        this.characterRepository = characterRepository;
        this.userRepository = userRepository;
        this.partyRepository = partyRepository;
        this.accessService = accessService;
    }

    public List<GameCharacter> findAll(AuthUser me, Integer guildId) {
        if (me.isAdmin()) {
            return guildId == null
                    ? characterRepository.findAll()
                    : characterRepository.findByGuildId(guildId);
        }
        if (me.isTeacher()) {
            if (guildId != null) {
                accessService.requireTeacherGuild(guildId);
                return characterRepository.findByGuildId(guildId);
            }
            return characterRepository.findByGuildIdIn(accessService.teacherGuildIds(me.getId()));
        }
        Integer g = accessService.requireStudentGuildContext(guildId);
        return characterRepository.findByGuildId(g);
    }

    /** Own characters across all guilds (no guild_id). Used by mobile student profile. */
    public List<GameCharacter> findMine(AuthUser me) {
        return characterRepository.findByUserId(me.getId());
    }

    public ResponseEntity<?> findById(AuthUser me, Integer id, Integer guildId) {
        return characterRepository.findById(id).<ResponseEntity<?>>map(c -> {
            if (me.isAdmin()) return ResponseEntity.ok(c);
            if (me.isTeacher()) {
                accessService.requireTeacherGuild(c.getGuildId());
                return ResponseEntity.ok(c);
            }
            Integer g = accessService.requireStudentGuildContext(guildId != null ? guildId : c.getGuildId());
            if (!Objects.equals(c.getGuildId(), g)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Out of scope"));
            }
            return ResponseEntity.ok(c);
        }).orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<?> create(AuthUser me, GameCharacter character) {
        if (me.isStudent()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Students cannot create characters"));
        }
        if (character.getUserId() == null || character.getGuildId() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "user_id and guild_id are required"));
        }
        if (me.isTeacher()) {
            accessService.requireTeacherGuild(character.getGuildId());
            User owner = userRepository.findById(character.getUserId()).orElse(null);
            if (owner == null || owner.getRole() != Role.Student) {
                return ResponseEntity.badRequest().body(Map.of("message", "user_id must be a Student"));
            }
        }
        character.setId(null);
        if (character.getPartyId() != null && character.getPartyId() <= 0) {
            character.setPartyId(null);
        }
        if (character.getPartyId() != null) {
            var party = partyRepository.findById(character.getPartyId()).orElse(null);
            if (party == null || !Objects.equals(party.getGuildId(), character.getGuildId())) {
                return ResponseEntity.badRequest().body(Map.of("message", "party must belong to the same guild"));
            }
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(characterRepository.save(character));
    }

    public ResponseEntity<?> update(AuthUser me, Integer id, Map<String, Object> data) {
        return characterRepository.findById(id).<ResponseEntity<?>>map(character -> {
            if (me.isStudent()) {
                if (!Objects.equals(character.getUserId(), me.getId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Can only edit your characters"));
                }
                if (character.getJob() == null && data.containsKey("job") && data.get("job") != null) {
                    // Allow setting job for the first time
                } else if (!data.keySet().stream().allMatch(key -> "name".equals(key))) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                            "message", "Students can only edit character name"
                    ));
                }
            } else if (me.isTeacher()) {
                accessService.requireTeacherGuild(character.getGuildId());
                if (!data.keySet().stream().allMatch(key -> "name".equals(key) || "party_id".equals(key))) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                            "message", "Teachers can only edit character name or party"
                    ));
                }
            } else if (!me.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Forbidden"));
            }

            if (data.containsKey("name") && data.get("name") != null) {
                character.setName(String.valueOf(data.get("name")));
            }
            if (data.containsKey("job")) {
                Object jobValue = data.get("job");
                if (jobValue == null || String.valueOf(jobValue).trim().isEmpty()) {
                    if (!me.isStudent()) {
                        character.setJob(null);
                    }
                } else {
                    character.setJob(String.valueOf(jobValue));
                }
            }
            if (data.containsKey("level") && data.get("level") != null) {
                character.setLevel(((Number) data.get("level")).intValue());
            }
            if (data.containsKey("exp") && data.get("exp") != null) {
                character.setExp(((Number) data.get("exp")).intValue());
            }
            if (me.isAdmin() || me.isTeacher()) {
                if (data.containsKey("user_id") && data.get("user_id") != null) {
                    character.setUserId(((Number) data.get("user_id")).intValue());
                }
                if (data.containsKey("guild_id") && data.get("guild_id") != null) {
                    Integer newGuild = ((Number) data.get("guild_id")).intValue();
                    if (me.isTeacher()) accessService.requireTeacherGuild(newGuild);
                    character.setGuildId(newGuild);
                }
            }
            if (data.containsKey("party_id")) {
                Object party = data.get("party_id");
                if (party == null || "".equals(String.valueOf(party).trim())) {
                    character.setPartyId(null);
                } else {
                    Integer partyId = ((Number) party).intValue();
                    var p = partyRepository.findById(partyId).orElse(null);
                    if (p == null || !Objects.equals(p.getGuildId(), character.getGuildId())) {
                        return ResponseEntity.badRequest().body(Map.of("message", "party must belong to the same guild"));
                    }
                    character.setPartyId(partyId);
                }
            }
            return ResponseEntity.ok(characterRepository.save(character));
        }).orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<?> delete(AuthUser me, Integer id) {
        return characterRepository.findById(id).<ResponseEntity<?>>map(character -> {
            if (me.isAdmin()) {
                characterRepository.delete(character);
                return ResponseEntity.noContent().build();
            }
            if (me.isTeacher()) {
                accessService.requireTeacherGuild(character.getGuildId());
                characterRepository.delete(character);
                return ResponseEntity.noContent().build();
            }
            if (me.isStudent() && Objects.equals(character.getUserId(), me.getId())) {
                characterRepository.delete(character);
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Forbidden"));
        }).orElse(ResponseEntity.notFound().build());
    }
}
