package dam.guildmaster.repository;

import dam.guildmaster.entity.Mentorship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MentorshipRepository extends JpaRepository<Mentorship, Mentorship.MentorshipId> {
    List<Mentorship> findByUserId(Integer userId);
    List<Mentorship> findByGuildId(Integer guildId);
    List<Mentorship> findByGuildIdIn(Collection<Integer> guildIds);
}
