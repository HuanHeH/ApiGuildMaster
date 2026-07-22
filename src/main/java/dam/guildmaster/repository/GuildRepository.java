package dam.guildmaster.repository;

import dam.guildmaster.entity.Guild;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuildRepository extends JpaRepository<Guild, Integer> {
}
