package dam.guildmaster.controller;

import dam.guildmaster.entity.GameEvent;
import dam.guildmaster.repository.EventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<GameEvent> create(@RequestBody GameEvent event) {
        event.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(eventRepository.save(event));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GameEvent> update(@PathVariable Integer id, @RequestBody GameEvent data) {
        return eventRepository.findById(id).map(event -> {
            if (data.getCasterCharacterId() != null) event.setCasterCharacterId(data.getCasterCharacterId());
            if (data.getSkillId() != null) event.setSkillId(data.getSkillId());
            if (data.getGuildId() != null) event.setGuildId(data.getGuildId());
            event.setTargetCharacterId(data.getTargetCharacterId());
            event.setTargetPartyId(data.getTargetPartyId());
            if (data.getStatus() != null) event.setStatus(data.getStatus());
            event.setReviewedByUserId(data.getReviewedByUserId());
            if (data.getComment() != null) event.setComment(data.getComment());
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
}
