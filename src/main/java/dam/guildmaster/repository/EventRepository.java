package dam.guildmaster.repository;

import dam.guildmaster.entity.GameEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<GameEvent, Integer> {
}
