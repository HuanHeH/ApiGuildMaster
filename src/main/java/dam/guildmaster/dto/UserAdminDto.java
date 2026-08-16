package dam.guildmaster.dto;

import dam.guildmaster.entity.User;
import dam.guildmaster.enums.Role;

/** Administrative user view. Password hashes must never leave the API. */
public class UserAdminDto {
    private Integer id;
    private String name;
    private String mail;
    private Role role;

    public UserAdminDto() {}

    public UserAdminDto(Integer id, String name, String mail, Role role) {
        this.id = id;
        this.name = name;
        this.mail = mail;
        this.role = role;
    }

    public static UserAdminDto from(User user) {
        return new UserAdminDto(user.getId(), user.getName(), user.getMail(), user.getRole());
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
