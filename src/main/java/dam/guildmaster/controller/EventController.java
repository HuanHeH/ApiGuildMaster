package dam.guildmaster.controller;

import dam.guildmaster.entity.GameEvent;
import dam.guildmaster.enums.EventStatus;
import dam.guildmaster.repository.EventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventRepository eventRepository;

    public EventController(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @GetMapping
    public List<GameEvent> getAll() {
        return eventRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameEvent> getById(@PathVariable Integer id) {
        return eventRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody GameEvent event) {
        event.setId(null);
        normalizeComment(event);
        ResponseEntity<?> invalid = validateCommentForStatus(event);
        if (invalid != null) return invalid;
        return ResponseEntity.status(HttpStatus.CREATED).body(eventRepository.save(event));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody GameEvent data) {
        return eventRepository.findById(id).<ResponseEntity<?>>map(event -> {
            if (data.getCasterCharacterId() != null) event.setCasterCharacterId(data.getCasterCharacterId());
            if (data.getSkillId() != null) event.setSkillId(data.getSkillId());
            if (data.getGuildId() != null) event.setGuildId(data.getGuildId());
            event.setTargetCharacterId(data.getTargetCharacterId());
            event.setTargetPartyId(data.getTargetPartyId());
            if (data.getStatus() != null) event.setStatus(data.getStatus());
            event.setReviewedByUserId(data.getReviewedByUserId());
            event.setComment(data.getComment());
            normalizeComment(event);
            ResponseEntity<?> invalid = validateCommentForStatus(event);
            if (invalid != null) return invalid;
            return ResponseEntity.ok(eventRepository.save(event));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!eventRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        eventRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private static void normalizeComment(GameEvent event) {
        String comment = event.getComment();
        if (comment != null && comment.isBlank()) {
            event.setComment(null);
        }
    }

    /** Comment required when Status is APPROVED or REJECTED; optional when PENDING. */
    private static ResponseEntity<Map<String, String>> validateCommentForStatus(GameEvent event) {
        EventStatus status = event.getStatus();
        if (status != null && status != EventStatus.PENDING) {
            String comment = event.getComment();
            if (comment == null || comment.isBlank()) {
                Map<String, String> body = new LinkedHashMap<>();
                body.put("message", "Comment is required when Status is APPROVED or REJECTED");
                return ResponseEntity.badRequest().body(body);
            }
        }
        return null;
    }
}
