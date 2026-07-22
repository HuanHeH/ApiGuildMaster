package dam.guildmaster.controller;

import dam.guildmaster.entity.Mentorship;
import dam.guildmaster.repository.MentorshipRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mentorships")
public class MentorshipController {

    private final MentorshipRepository mentorshipRepository;

    public MentorshipController(MentorshipRepository mentorshipRepository) {
        this.mentorshipRepository = mentorshipRepository;
    }

    @GetMapping
    public List<Mentorship> getAll() {
        return mentorshipRepository.findAll();
    }

    @GetMapping("/{userId}/{guildId}")
    public ResponseEntity<Mentorship> getById(@PathVariable Integer userId, @PathVariable Integer guildId) {
        return mentorshipRepository.findById(new Mentorship.MentorshipId(userId, guildId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Mentorship> create(@RequestBody Mentorship mentorship) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mentorshipRepository.save(mentorship));
    }

    @PutMapping("/{userId}/{guildId}")
    public ResponseEntity<Mentorship> update(
            @PathVariable Integer userId,
            @PathVariable Integer guildId,
            @RequestBody Mentorship data) {
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

    @DeleteMapping("/{userId}/{guildId}")
    public ResponseEntity<Void> delete(@PathVariable Integer userId, @PathVariable Integer guildId) {
        Mentorship.MentorshipId id = new Mentorship.MentorshipId(userId, guildId);
        if (!mentorshipRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        mentorshipRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
