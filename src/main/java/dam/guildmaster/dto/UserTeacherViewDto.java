package dam.guildmaster.dto;

import dam.guildmaster.enums.Role;
import dam.guildmaster.entity.User;

/** Teacher view of students — mail visible, hash never. */
public class UserTeacherViewDto {
    private Integer id;
    private String name;
    private String mail;
    private Role role;

    public UserTeacherViewDto() {}

    public UserTeacherViewDto(Integer id, String name, String mail, Role role) {
        this.id = id;
        this.name = name;
        this.mail = mail;
        this.role = role;
    }

    public static UserTeacherViewDto from(User u) {
        return new UserTeacherViewDto(u.getId(), u.getName(), u.getMail(), u.getRole());
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
