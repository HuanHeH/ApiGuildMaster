package dam.guildmaster.service;

import dam.guildmaster.entity.Skill;
import dam.guildmaster.repository.SkillRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SkillService {

    private final SkillRepository skillRepository;

    public SkillService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    public List<Skill> findAll() {
        return skillRepository.findAll();
    }

    public ResponseEntity<Skill> findById(Integer id) {
        return skillRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<Skill> create(Skill skill) {
        skill.setId(null);
        if (skill.getDebuff() == null) skill.setDebuff(false);
        return ResponseEntity.status(HttpStatus.CREATED).body(skillRepository.save(skill));
    }

    public ResponseEntity<Skill> update(Integer id, Skill data) {
        return skillRepository.findById(id).map(skill -> {
            if (data.getName() != null) skill.setName(data.getName());
            if (data.getLevelReq() != null) skill.setLevelReq(data.getLevelReq());
            if (data.getJob() != null) skill.setJob(data.getJob());
            if (data.getDescription() != null) skill.setDescription(data.getDescription());
            if (data.getAoe() != null) skill.setAoe(data.getAoe());
            if (data.getExpCost() != null) skill.setExpCost(data.getExpCost());
            if (data.getDebuff() != null) skill.setDebuff(data.getDebuff());
            return ResponseEntity.ok(skillRepository.save(skill));
        }).orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<Void> delete(Integer id) {
        if (!skillRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        skillRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
