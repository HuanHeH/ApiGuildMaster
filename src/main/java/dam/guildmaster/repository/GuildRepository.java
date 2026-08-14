package dam.guildmaster.repository;

import dam.guildmaster.entity.Guild;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface GuildRepository extends JpaRepository<Guild, Integer> {
    List<Guild> findByIdIn(Collection<Integer> ids);
}
