package dam.guildmaster.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Characters")
public class GameCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "Name", nullable = false, length = 100)
    private String name;

    @Column(name = "Job", nullable = true, length = 50)
    private String job;

    @Column(name = "Level", nullable = false)
    private Integer level = 1;

    @Column(name = "Exp", nullable = false)
    private Integer exp = 0;

    @Column(name = "UserID", nullable = false)
    private Integer userId;

    @Column(name = "GuildID", nullable = false)
    private Integer guildId;

    @Column(name = "PartyID", nullable = true)
    private Integer partyId;

    public GameCharacter() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getJob() { return job; }
    public void setJob(String job) { this.job = job; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public Integer getExp() { return exp; }
    public void setExp(Integer exp) { this.exp = exp; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getGuildId() { return guildId; }
    public void setGuildId(Integer guildId) { this.guildId = guildId; }

    public Integer getPartyId() { return partyId; }
    public void setPartyId(Integer partyId) { this.partyId = partyId; }
}
