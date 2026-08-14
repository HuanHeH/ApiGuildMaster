package dam.guildmaster.repository;

import dam.guildmaster.entity.Party;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PartyRepository extends JpaRepository<Party, Integer> {
    List<Party> findByGuildId(Integer guildId);
    List<Party> findByGuildIdIn(Collection<Integer> guildIds);
}
