package dam.guildmaster.repository;

import dam.guildmaster.entity.GameEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface EventRepository extends JpaRepository<GameEvent, Integer> {
    List<GameEvent> findByGuildId(Integer guildId);
    List<GameEvent> findByGuildIdIn(Collection<Integer> guildIds);
}
