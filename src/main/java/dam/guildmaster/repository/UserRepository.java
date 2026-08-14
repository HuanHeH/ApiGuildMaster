package dam.guildmaster.repository;

import dam.guildmaster.entity.User;
import dam.guildmaster.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByMailIgnoreCase(String mail);
    List<User> findByIdIn(Collection<Integer> ids);
    List<User> findByRole(Role role);
}
