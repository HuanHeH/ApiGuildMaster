package dam.guildmaster.service;

import dam.guildmaster.entity.Mentorship;
import dam.guildmaster.repository.MentorshipRepository;
import dam.guildmaster.security.AccessService;
import dam.guildmaster.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MentorshipService {

    private final MentorshipRepository mentorshipRepository;
    private final AccessService accessService;

    public MentorshipService(MentorshipRepository mentorshipRepository, AccessService accessService) {
        this.mentorshipRepository = mentorshipRepository;
        this.accessService = accessService;
    }

    public ResponseEntity<?> findAll(AuthUser me) {
        if (me.isAdmin()) {
            return ResponseEntity.ok(mentorshipRepository.findAll());
        }
        if (me.isTeacher()) {
            return ResponseEntity.ok(mentorshipRepository.findByUserId(me.getId()));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Students cannot list mentorships; use /users?guild_id="));
    }

    public ResponseEntity<?> findById(AuthUser me, Integer userId, Integer guildId) {
        if (me.isStudent()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Forbidden"));
        }
        if (me.isTeacher() && !me.getId().equals(userId)) {
            accessService.requireTeacherGuild(guildId);
        }
        return mentorshipRepository.findById(new Mentorship.MentorshipId(userId, guildId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<Mentorship> create(Mentorship mentorship) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mentorshipRepository.save(mentorship));
    }

    public ResponseEntity<?> update(Integer userId, Integer guildId, Mentorship data) {
        Mentorship.MentorshipId oldId = new Mentorship.MentorshipId(userId, guildId);
        if (!mentorshipRepository.existsById(oldId)) {
            return ResponseEntity.notFound().build();
        }
        Integer newUserId = data.getUserId() != null ? data.getUserId() : userId;
        Integer newGuildId = data.getGuildId() != null ? data.getGuildId() : guildId;
        if (!oldId.equals(new Mentorship.MentorshipId(newUserId, newGuildId))) {
            mentorshipRepository.deleteById(oldId);
        }
        Mentorship updated = new Mentorship();
        updated.setUserId(newUserId);
        updated.setGuildId(newGuildId);
        return ResponseEntity.ok(mentorshipRepository.save(updated));
    }

    public ResponseEntity<Void> delete(Integer userId, Integer guildId) {
        Mentorship.MentorshipId id = new Mentorship.MentorshipId(userId, guildId);
        if (!mentorshipRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        mentorshipRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
