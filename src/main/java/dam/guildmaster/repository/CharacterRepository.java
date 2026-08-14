package dam.guildmaster.repository;

import dam.guildmaster.entity.GameCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CharacterRepository extends JpaRepository<GameCharacter, Integer> {
    List<GameCharacter> findByUserId(Integer userId);
    List<GameCharacter> findByGuildId(Integer guildId);
    List<GameCharacter> findByUserIdAndGuildId(Integer userId, Integer guildId);
    List<GameCharacter> findByGuildIdIn(Collection<Integer> guildIds);
    List<GameCharacter> findByPartyId(Integer partyId);
}
