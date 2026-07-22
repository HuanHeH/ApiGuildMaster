package dam.guildmaster.controller;

import dam.guildmaster.entity.Party;
import dam.guildmaster.repository.PartyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parties")
public class PartyController {

    private final PartyRepository partyRepository;

    public PartyController(PartyRepository partyRepository) {
        this.partyRepository = partyRepository;
    }

    @GetMapping
    public List<Party> getAll() {
        return partyRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Party> getById(@PathVariable Integer id) {
        return partyRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Party> create(@RequestBody Party party) {
        party.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(partyRepository.save(party));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Party> update(@PathVariable Integer id, @RequestBody Party data) {
        return partyRepository.findById(id).map(party -> {
            if (data.getName() != null) party.setName(data.getName());
            if (data.getGuildId() != null) party.setGuildId(data.getGuildId());
            return ResponseEntity.ok(partyRepository.save(party));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!partyRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        partyRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
