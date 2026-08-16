package dam.guildmaster.service;

import dam.guildmaster.entity.GameCharacter;
import dam.guildmaster.enums.Role;
import dam.guildmaster.repository.CharacterRepository;
import dam.guildmaster.repository.PartyRepository;
import dam.guildmaster.repository.UserRepository;
import dam.guildmaster.security.AccessService;
import dam.guildmaster.security.AuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CharacterServiceTest {

    @Mock
    private CharacterRepository characterRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private AccessService accessService;

    private CharacterService characterService;

    @BeforeEach
    void setUp() {
        characterService = new CharacterService(
                characterRepository,
                userRepository,
                partyRepository,
                accessService
        );
    }

    @Test
    void studentCannotChangeProgressionFields() {
        GameCharacter character = character(7, 42, 3, 10);
        when(characterRepository.findById(7)).thenReturn(Optional.of(character));

        AuthUser student = new AuthUser(42, "student@example.test", "Student", Role.Student, "jti");
        ResponseEntity<?> response = characterService.update(student, 7, Map.of("exp", 999));

        assertEquals(403, response.getStatusCode().value());
        assertEquals(10, character.getExp());
        verify(characterRepository, never()).save(character);
    }

    @Test
    void studentCanChangeOnlyName() {
        GameCharacter character = character(7, 42, 3, 10);
        when(characterRepository.findById(7)).thenReturn(Optional.of(character));
        when(characterRepository.save(character)).thenReturn(character);

        AuthUser student = new AuthUser(42, "student@example.test", "Student", Role.Student, "jti");
        ResponseEntity<?> response = characterService.update(student, 7, Map.of("name", "New name"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("New name", character.getName());
        verify(characterRepository).save(character);
    }

    private static GameCharacter character(int id, int userId, int level, int exp) {
        GameCharacter character = new GameCharacter();
        character.setId(id);
        character.setUserId(userId);
        character.setGuildId(9);
        character.setName("Character");
        character.setJob("Mage");
        character.setLevel(level);
        character.setExp(exp);
        return character;
    }
}
