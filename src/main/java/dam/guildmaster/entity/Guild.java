package dam.guildmaster.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Guilds")
public class Guild {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    /** Display name, e.g. guild nickname chosen by students */
    @Column(name = "Name", nullable = false, length = 100, unique = true)
    private String name;

    /** Course year: 1, 2, 3, 4... */
    @Column(name = "Number", nullable = false)
    private Integer number;

    /** Group letter: A, B, C... (required) */
    @Column(name = "Letter", nullable = false, length = 1)
    private String letter;

    /** Stage: ESO, Bachillerato, FP... (optional = NULL) */
    @Column(name = "Level", length = 50)
    private String level;

    /** Specialty/track (optional = NULL) */
    @Column(name = "Modality", length = 50)
    private String modality;

    public Guild() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getNumber() { return number; }
    public void setNumber(Integer number) { this.number = number; }

    public String getLetter() { return letter; }
    public void setLetter(String letter) { this.letter = letter; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getModality() { return modality; }
    public void setModality(String modality) { this.modality = modality; }
}
