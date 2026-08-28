package dam.guildmaster.service;

import dam.guildmaster.entity.Party;
import dam.guildmaster.repository.PartyRepository;
import dam.guildmaster.security.AccessService;
import dam.guildmaster.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PartyService {

    private final PartyRepository partyRepository;
    private final AccessService accessService;

    public PartyService(PartyRepository partyRepository, AccessService accessService) {
        this.partyRepository = partyRepository;
        this.accessService = accessService;
    }

    public List<Party> findAll(AuthUser me, Integer guildId) {
        if (me.isAdmin()) {
            return guildId == null ? partyRepository.findAll() : partyRepository.findByGuildId(guildId);
        }
        if (me.isTeacher()) {
            if (guildId != null) {
                accessService.requireTeacherGuild(guildId);
                return partyRepository.findByGuildId(guildId);
            }
            return partyRepository.findByGuildIdIn(accessService.teacherGuildIds(me.getId()));
        }
        Integer g = accessService.requireStudentGuildContext(guildId);
        return partyRepository.findByGuildId(g);
    }

    public ResponseEntity<?> findById(AuthUser me, Integer id, Integer guildId) {
        return partyRepository.findById(id).<ResponseEntity<?>>map(p -> {
            if (me.isAdmin()) return ResponseEntity.ok(p);
            if (me.isTeacher()) {
                accessService.requireTeacherGuild(p.getGuildId());
                return ResponseEntity.ok(p);
            }
            Integer g = accessService.requireStudentGuildContext(guildId != null ? guildId : p.getGuildId());
            if (!Objects.equals(p.getGuildId(), g)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Out of scope"));
            }
            return ResponseEntity.ok(p);
        }).orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<?> create(AuthUser me, Party party) {
        if (party.getName() == null || party.getName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "name is required"));
        }
        if (party.getName().length() > 100) {
            return ResponseEntity.badRequest().body(Map.of("message", "name must be 100 characters or fewer"));
        }
        if (party.getGuildId() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "guild_id is required"));
        }
        if (me.isAdmin()) {
            // OK
        } else if (me.isTeacher()) {
            accessService.requireTeacherGuild(party.getGuildId());
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Forbidden"));
        }
        party.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(partyRepository.save(party));
    }

    public ResponseEntity<?> update(AuthUser me, Integer id, Party data) {
        return partyRepository.findById(id).<ResponseEntity<?>>map(party -> {
            if (!me.isAdmin()) {
                accessService.requireTeacherGuild(party.getGuildId());
            }
            if (data.getName() != null) {
                if (data.getName().isBlank()) {
                    return ResponseEntity.badRequest().body(Map.of("message", "name cannot be blank"));
                }
                if (data.getName().length() > 100) {
                    return ResponseEntity.badRequest().body(Map.of("message", "name must be 100 characters or fewer"));
                }
                party.setName(data.getName());
            }
            if (data.getGuildId() != null && !Objects.equals(data.getGuildId(), party.getGuildId())) {
                if (me.isTeacher()) {
                    accessService.requireTeacherGuild(data.getGuildId());
                }
                party.setGuildId(data.getGuildId());
            }
            return ResponseEntity.ok(partyRepository.save(party));
        }).orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<?> delete(AuthUser me, Integer id) {
        return partyRepository.findById(id).<ResponseEntity<?>>map(party -> {
            if (!me.isAdmin()) {
                if (!me.isTeacher()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Forbidden"));
                }
                accessService.requireTeacherGuild(party.getGuildId());
            }
            partyRepository.delete(party);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
