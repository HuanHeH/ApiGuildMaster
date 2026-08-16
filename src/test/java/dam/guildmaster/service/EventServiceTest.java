package dam.guildmaster.service;

import dam.guildmaster.entity.GameCharacter;
import dam.guildmaster.entity.GameEvent;
import dam.guildmaster.entity.Skill;
import dam.guildmaster.enums.Aoe;
import dam.guildmaster.enums.Role;
import dam.guildmaster.repository.CharacterRepository;
import dam.guildmaster.repository.EventRepository;
import dam.guildmaster.repository.PartyRepository;
import dam.guildmaster.repository.SkillRepository;
import dam.guildmaster.security.AccessService;
import dam.guildmaster.security.AuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CharacterRepository characterRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private AccessService accessService;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService(
                eventRepository,
                characterRepository,
                skillRepository,
                partyRepository,
                accessService
        );
    }

    @Test
    void insufficientExpDoesNotApplyLevelUp() {
        GameCharacter caster = new GameCharacter();
        caster.setId(1);
        caster.setUserId(42);
        caster.setGuildId(9);
        caster.setJob("Mage");
        caster.setLevel(1);
        caster.setExp(0);

        Skill levelUp = new Skill();
        levelUp.setId(2);
        levelUp.setName("Level Up I");
        levelUp.setLevelReq(1);
        levelUp.setJob("Common");
        levelUp.setAoe(Aoe.SINGLE);
        levelUp.setExpCost(5);
        levelUp.setDebuff(false);

        GameEvent event = new GameEvent();
        event.setCasterCharacterId(1);
        event.setSkillId(2);
        event.setGuildId(9);
        event.setTargetCharacterId(1);

        when(characterRepository.findById(1)).thenReturn(Optional.of(caster));
        when(skillRepository.findById(2)).thenReturn(Optional.of(levelUp));

        AuthUser student = new AuthUser(42, "student@example.test", "Student", Role.Student, "jti");
        ResponseEntity<?> response = eventService.create(student, event);

        assertEquals(400, response.getStatusCode().value());
        assertEquals(1, caster.getLevel());
        assertEquals(0, caster.getExp());
        verify(characterRepository, never()).save(caster);
        verify(eventRepository, never()).save(event);
    }
}
