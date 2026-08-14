package dam.guildmaster.entity;

import dam.guildmaster.enums.Aoe;
import jakarta.persistence.*;

@Entity
@Table(name = "Skills")
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "Name", nullable = false, length = 100)
    private String name;

    @Column(name = "LevelReq", nullable = false)
    private Integer levelReq;

    @Column(name = "Job", nullable = false, length = 50)
    private String job;

    @Column(name = "Description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "AOE", nullable = false)
    private Aoe aoe;

    @Column(name = "ExpCost", nullable = false)
    private Integer expCost;

    /** True = debuff / hostile skill (flag for future C5c / caps logic). */
    @Column(name = "Debuff", nullable = false)
    private Boolean debuff = false;

    public Skill() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getLevelReq() { return levelReq; }
    public void setLevelReq(Integer levelReq) { this.levelReq = levelReq; }

    public String getJob() { return job; }
    public void setJob(String job) { this.job = job; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Aoe getAoe() { return aoe; }
    public void setAoe(Aoe aoe) { this.aoe = aoe; }

    public Integer getExpCost() { return expCost; }
    public void setExpCost(Integer expCost) { this.expCost = expCost; }

    public Boolean getDebuff() { return debuff; }
    public void setDebuff(Boolean debuff) { this.debuff = debuff; }
}
