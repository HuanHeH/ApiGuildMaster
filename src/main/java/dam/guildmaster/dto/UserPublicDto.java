package dam.guildmaster.dto;

import dam.guildmaster.enums.Role;
import dam.guildmaster.entity.User;

/** Student view of classmates / teachers — no mail, no hash. */
public class UserPublicDto {
    private Integer id;
    private String name;
    private Role role;

    public UserPublicDto() {}

    public UserPublicDto(Integer id, String name, Role role) {
        this.id = id;
        this.name = name;
        this.role = role;
    }

    public static UserPublicDto from(User u) {
        return new UserPublicDto(u.getId(), u.getName(), u.getRole());
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
