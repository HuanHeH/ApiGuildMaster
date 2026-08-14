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

    public ResponseEntity<Party> create(Party party) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partyRepository.save(party));
    }

    public ResponseEntity<?> update(Integer id, Party data) {
        return partyRepository.findById(id).map(party -> {
            if (data.getName() != null) party.setName(data.getName());
            if (data.getGuildId() != null) party.setGuildId(data.getGuildId());
            return ResponseEntity.ok(partyRepository.save(party));
        }).orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<Void> delete(Integer id) {
        if (!partyRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        partyRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
