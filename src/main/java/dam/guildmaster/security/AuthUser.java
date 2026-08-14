package dam.guildmaster.security;

import dam.guildmaster.enums.Role;

public class AuthUser {
    private final Integer id;
    private final String mail;
    private final String name;
    private final Role role;
    private final String jti;

    public AuthUser(Integer id, String mail, String name, Role role, String jti) {
        this.id = id;
        this.mail = mail;
        this.name = name;
        this.role = role;
        this.jti = jti;
    }

    public Integer getId() { return id; }
    public String getMail() { return mail; }
    public String getName() { return name; }
    public Role getRole() { return role; }
    public String getJti() { return jti; }

    public boolean isAdmin() { return role == Role.Admin; }
    public boolean isTeacher() { return role == Role.Teacher; }
    public boolean isStudent() { return role == Role.Student; }
}
