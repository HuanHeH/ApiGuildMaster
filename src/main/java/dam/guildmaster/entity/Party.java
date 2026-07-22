package dam.guildmaster.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Parties")
public class Party {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "Name", nullable = false, length = 100, unique = true)
    private String name;

    @Column(name = "GuildID", nullable = false)
    private Integer guildId;

    public Party() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getGuildId() { return guildId; }
    public void setGuildId(Integer guildId) { this.guildId = guildId; }
}
