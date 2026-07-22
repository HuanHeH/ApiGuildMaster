package dam.guildmaster.entity;

import dam.guildmaster.enums.Role;
import jakarta.persistence.*;

@Entity
@Table(name = "Users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "Name", nullable = false, length = 100)
    private String name;

    @Column(name = "Mail", nullable = false, length = 150, unique = true)
    private String mail;

    @Column(name = "Hash", nullable = false, length = 255)
    private String hash;

    @Enumerated(EnumType.STRING)
    @Column(name = "Role", nullable = false)
    private Role role;

    public User() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
