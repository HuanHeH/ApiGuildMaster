package dam.guildmaster.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import dam.guildmaster.entity.User;
import dam.guildmaster.enums.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAdminDtoTest {

    @Test
    void doesNotSerializePasswordHash() throws Exception {
        User user = new User();
        user.setId(1);
        user.setName("Admin");
        user.setMail("admin@example.test");
        user.setHash("$2b$12$secret");
        user.setRole(Role.Admin);

        String json = new ObjectMapper().writeValueAsString(UserAdminDto.from(user));

        assertTrue(json.contains("admin@example.test"));
        assertFalse(json.contains("hash"));
        assertFalse(json.contains("secret"));
    }
}
