package dam.guildmaster.service;

import dam.guildmaster.dto.LoginRequest;
import dam.guildmaster.entity.User;
import dam.guildmaster.enums.Role;
import dam.guildmaster.repository.CharacterRepository;
import dam.guildmaster.repository.MentorshipRepository;
import dam.guildmaster.repository.UserRepository;
import dam.guildmaster.security.AccessService;
import dam.guildmaster.security.AuthUser;
import dam.guildmaster.security.JwtService;
import dam.guildmaster.security.LoginRateLimiter;
import dam.guildmaster.security.SessionActivityStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CharacterRepository characterRepository;

    @Mock
    private MentorshipRepository mentorshipRepository;

    @Mock
    private AccessService accessService;

    private UserService userService;
    private SessionActivityStore sessionActivityStore;

    @BeforeEach
    void setUp() {
        sessionActivityStore = new SessionActivityStore(30);
        userService = new UserService(
                userRepository,
                characterRepository,
                mentorshipRepository,
                new BCryptPasswordEncoder(),
                new JwtService("test-secret-with-at-least-32-bytes-long", 16),
                sessionActivityStore,
                new LoginRateLimiter(5, 60, 300),
                accessService
        );
    }

    @Test
    void invalidLoginIsRateLimited() {
        when(userRepository.findByMailIgnoreCase("user@example.test")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setMail("user@example.test");
        request.setPassword("wrong");

        ResponseEntity<?> response = null;
        for (int i = 0; i < 5; i++) {
            response = userService.login(request, "127.0.0.1");
        }

        assertEquals(429, response.getStatusCode().value());
    }

    @Test
    void logoutRevokesTheCurrentSession() {
        sessionActivityStore.register("logout-jti");

        AuthUser user = new AuthUser(1, "user@example.test", "User", Role.Student, "logout-jti");
        ResponseEntity<Void> response = userService.logout(user);

        assertEquals(204, response.getStatusCode().value());
        org.junit.jupiter.api.Assertions.assertFalse(sessionActivityStore.touch("logout-jti"));
    }

    @Test
    void touchRehydratesUnknownJtiAfterRestart() {
        org.junit.jupiter.api.Assertions.assertTrue(sessionActivityStore.touch("fresh-jti"));
    }
}
