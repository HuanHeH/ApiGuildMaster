package dam.guildmaster.controller;

import dam.guildmaster.entity.Skill;
import dam.guildmaster.repository.SkillRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillRepository skillRepository;

    public SkillController(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    @GetMapping
    public List<Skill> getAll() {
        return skillRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Skill> getById(@PathVariable Integer id) {
        return skillRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Skill> create(@RequestBody Skill skill) {
        skill.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(skillRepository.save(skill));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Skill> update(@PathVariable Integer id, @RequestBody Skill data) {
        return skillRepository.findById(id).map(skill -> {
            if (data.getName() != null) skill.setName(data.getName());
            if (data.getLevelReq() != null) skill.setLevelReq(data.getLevelReq());
            if (data.getJob() != null) skill.setJob(data.getJob());
            if (data.getDescription() != null) skill.setDescription(data.getDescription());
            if (data.getAoe() != null) skill.setAoe(data.getAoe());
            if (data.getExpCost() != null) skill.setExpCost(data.getExpCost());
            return ResponseEntity.ok(skillRepository.save(skill));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!skillRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        skillRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
