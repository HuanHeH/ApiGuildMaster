package dam.guildmaster.controller;

import dam.guildmaster.entity.Skill;
import dam.guildmaster.security.AccessService;
import dam.guildmaster.service.SkillService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillService skillService;
    private final AccessService accessService;

    public SkillController(SkillService skillService, AccessService accessService) {
        this.skillService = skillService;
        this.accessService = accessService;
    }

    @GetMapping
    public List<Skill> getAll() {
        accessService.requireUser();
        return skillService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Skill> getById(@PathVariable Integer id) {
        accessService.requireUser();
        return skillService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Skill> create(@RequestBody Skill skill) {
        accessService.requireAdmin();
        return skillService.create(skill);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Skill> update(@PathVariable Integer id, @RequestBody Skill data) {
        accessService.requireAdmin();
        return skillService.update(id, data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        accessService.requireAdmin();
        return skillService.delete(id);
    }
}
