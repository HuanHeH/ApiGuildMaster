package dam.guildmaster.security;

import dam.guildmaster.entity.GameCharacter;
import dam.guildmaster.entity.Mentorship;
import dam.guildmaster.enums.Role;
import dam.guildmaster.repository.CharacterRepository;
import dam.guildmaster.repository.MentorshipRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AccessService {

    private final CharacterRepository characterRepository;
    private final MentorshipRepository mentorshipRepository;

    public AccessService(CharacterRepository characterRepository, MentorshipRepository mentorshipRepository) {
        this.characterRepository = characterRepository;
        this.mentorshipRepository = mentorshipRepository;
    }

    public AuthUser requireUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return user;
    }

    public List<Integer> studentGuildIds(Integer userId) {
        return characterRepository.findByUserId(userId).stream()
                .map(GameCharacter::getGuildId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    public List<Integer> teacherGuildIds(Integer userId) {
        return mentorshipRepository.findByUserId(userId).stream()
                .map(Mentorship::getGuildId)
                .distinct()
                .toList();
    }

    public List<Integer> scopedGuildIds(AuthUser user) {
        if (user.isAdmin()) {
            return null; // null = all
        }
        if (user.isTeacher()) {
            return teacherGuildIds(user.getId());
        }
        return studentGuildIds(user.getId());
    }

    /** Student must pass guild_id they belong to. Teacher/Admin: optional filter. */
    public Integer requireStudentGuildContext(Integer guildId) {
        AuthUser user = requireUser();
        if (!user.isStudent()) {
            return guildId;
        }
        if (guildId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "guild_id is required for students");
        }
        if (!studentGuildIds(user.getId()).contains(guildId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No character in that guild");
        }
        return guildId;
    }

    public void requireTeacherGuild(Integer guildId) {
        AuthUser user = requireUser();
        if (user.isAdmin()) return;
        if (!user.isTeacher()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teachers only");
        }
        if (guildId == null || !teacherGuildIds(user.getId()).contains(guildId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No mentorship in that guild");
        }
    }

    public boolean canAccessGuild(AuthUser user, Integer guildId) {
        if (user.isAdmin()) return true;
        if (guildId == null) return false;
        List<Integer> scope = scopedGuildIds(user);
        return scope != null && scope.contains(guildId);
    }

    public void assertCanAccessGuild(Integer guildId) {
        AuthUser user = requireUser();
        if (!canAccessGuild(user, guildId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Guild out of scope");
        }
    }

    public Set<Integer> myCharacterIdsInGuild(AuthUser user, Integer guildId) {
        return characterRepository.findByUserIdAndGuildId(user.getId(), guildId).stream()
                .map(GameCharacter::getId)
                .collect(Collectors.toSet());
    }

    public Set<Integer> myPartyIdsInGuild(AuthUser user, Integer guildId) {
        return characterRepository.findByUserIdAndGuildId(user.getId(), guildId).stream()
                .map(GameCharacter::getPartyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public void requireAdmin() {
        if (!requireUser().isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin only");
        }
    }

    public void requireAdminOrTeacher() {
        AuthUser u = requireUser();
        if (!u.isAdmin() && !u.isTeacher()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin or Teacher only");
        }
    }
}
