package dam.guildmaster.repository;

import dam.guildmaster.entity.GameCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterRepository extends JpaRepository<GameCharacter, Integer> {
}
