package dam.guildmaster.repository;

import dam.guildmaster.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByMailIgnoreCase(String mail);
}
