package dam.guildmaster.repository;

import dam.guildmaster.entity.Mentorship;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MentorshipRepository extends JpaRepository<Mentorship, Mentorship.MentorshipId> {
}
