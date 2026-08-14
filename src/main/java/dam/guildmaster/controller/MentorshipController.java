package dam.guildmaster.controller;

import dam.guildmaster.entity.Mentorship;
import dam.guildmaster.security.AccessService;
import dam.guildmaster.security.AuthUser;
import dam.guildmaster.service.MentorshipService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mentorships")
public class MentorshipController {

    private final MentorshipService mentorshipService;
    private final AccessService accessService;

    public MentorshipController(MentorshipService mentorshipService, AccessService accessService) {
        this.mentorshipService = mentorshipService;
        this.accessService = accessService;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        AuthUser me = accessService.requireUser();
        return mentorshipService.findAll(me);
    }

    @GetMapping("/{userId}/{guildId}")
    public ResponseEntity<?> getById(@PathVariable Integer userId, @PathVariable Integer guildId) {
        AuthUser me = accessService.requireUser();
        return mentorshipService.findById(me, userId, guildId);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Mentorship mentorship) {
        accessService.requireAdmin();
        return mentorshipService.create(mentorship);
    }

    @PutMapping("/{userId}/{guildId}")
    public ResponseEntity<?> update(
            @PathVariable Integer userId,
            @PathVariable Integer guildId,
            @RequestBody Mentorship data) {
        accessService.requireAdmin();
        return mentorshipService.update(userId, guildId, data);
    }

    @DeleteMapping("/{userId}/{guildId}")
    public ResponseEntity<?> delete(@PathVariable Integer userId, @PathVariable Integer guildId) {
        accessService.requireAdmin();
        return mentorshipService.delete(userId, guildId);
    }
}
