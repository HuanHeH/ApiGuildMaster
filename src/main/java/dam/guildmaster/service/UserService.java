package dam.guildmaster.service;

import dam.guildmaster.dto.LoginRequest;
import dam.guildmaster.dto.LoginResponse;
import dam.guildmaster.dto.UserPublicDto;
import dam.guildmaster.dto.UserTeacherViewDto;
import dam.guildmaster.entity.GameCharacter;
import dam.guildmaster.entity.Mentorship;
import dam.guildmaster.entity.User;
import dam.guildmaster.enums.Role;
import dam.guildmaster.repository.CharacterRepository;
import dam.guildmaster.repository.MentorshipRepository;
import dam.guildmaster.repository.UserRepository;
import dam.guildmaster.security.AccessService;
import dam.guildmaster.security.AuthUser;
import dam.guildmaster.security.JwtService;
import dam.guildmaster.security.SessionActivityStore;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final MentorshipRepository mentorshipRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SessionActivityStore sessionActivityStore;
    private final AccessService accessService;

    public UserService(
            UserRepository userRepository,
            CharacterRepository characterRepository,
            MentorshipRepository mentorshipRepository,
            BCryptPasswordEncoder passwordEncoder,
            JwtService jwtService,
            SessionActivityStore sessionActivityStore,
            AccessService accessService) {
        this.userRepository = userRepository;
        this.characterRepository = characterRepository;
        this.mentorshipRepository = mentorshipRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.sessionActivityStore = sessionActivityStore;
        this.accessService = accessService;
    }

    public ResponseEntity<?> findAll(AuthUser me, Integer guildId) {
        if (me.isAdmin()) {
            return ResponseEntity.ok(userRepository.findAll());
        }
        if (me.isTeacher()) {
            Integer g = guildId;
            if (g == null) {
                List<Integer> guilds = accessService.teacherGuildIds(me.getId());
                Set<Integer> studentIds = characterRepository.findByGuildIdIn(guilds).stream()
                        .map(GameCharacter::getUserId)
                        .collect(Collectors.toSet());
                Set<Integer> teacherIds = mentorshipRepository.findByGuildIdIn(guilds).stream()
                        .map(Mentorship::getUserId)
                        .collect(Collectors.toSet());
                Set<Integer> ids = new HashSet<>();
                ids.addAll(studentIds);
                ids.addAll(teacherIds);
                List<UserTeacherViewDto> out = userRepository.findByIdIn(ids).stream()
                        .filter(u -> u.getRole() == Role.Student || u.getRole() == Role.Teacher)
                        .map(UserTeacherViewDto::from)
                        .toList();
                return ResponseEntity.ok(out);
            }
            accessService.requireTeacherGuild(g);
            Set<Integer> studentIds = characterRepository.findByGuildId(g).stream()
                    .map(GameCharacter::getUserId)
                    .collect(Collectors.toSet());
            Set<Integer> teacherIds = mentorshipRepository.findByGuildId(g).stream()
                    .map(Mentorship::getUserId)
                    .collect(Collectors.toSet());
            Set<Integer> ids = new HashSet<>();
            ids.addAll(studentIds);
            ids.addAll(teacherIds);
            List<UserTeacherViewDto> out = userRepository.findByIdIn(ids).stream()
                    .filter(u -> u.getRole() == Role.Student || u.getRole() == Role.Teacher)
                    .map(UserTeacherViewDto::from)
                    .toList();
            return ResponseEntity.ok(out);
        }
        Integer g = accessService.requireStudentGuildContext(guildId);
        Set<Integer> classmateIds = characterRepository.findByGuildId(g).stream()
                .map(GameCharacter::getUserId)
                .collect(Collectors.toSet());
        Set<Integer> teacherIds = mentorshipRepository.findByGuildId(g).stream()
                .map(Mentorship::getUserId)
                .collect(Collectors.toSet());
        Set<Integer> ids = new HashSet<>();
        ids.addAll(classmateIds);
        ids.addAll(teacherIds);
        List<UserPublicDto> out = userRepository.findByIdIn(ids).stream()
                .map(UserPublicDto::from)
                .toList();
        return ResponseEntity.ok(out);
    }

    public ResponseEntity<?> findById(AuthUser me, Integer id, Integer guildId) {
        return userRepository.findById(id).<ResponseEntity<?>>map(user -> {
            if (me.isAdmin()) {
                return ResponseEntity.ok(user);
            }
            if (me.isTeacher()) {
                List<Integer> guilds = accessService.teacherGuildIds(me.getId());
                boolean studentInScope = user.getRole() == Role.Student
                        && characterRepository.findByUserId(user.getId()).stream()
                        .anyMatch(c -> guilds.contains(c.getGuildId()));
                boolean teacherInScope = user.getRole() == Role.Teacher
                        && mentorshipRepository.findByUserId(user.getId()).stream()
                        .anyMatch(m -> guilds.contains(m.getGuildId()));
                if (!studentInScope && !teacherInScope) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Out of scope"));
                }
                return ResponseEntity.ok(UserTeacherViewDto.from(user));
            }
            Integer g = accessService.requireStudentGuildContext(guildId);
            boolean classmate = characterRepository.findByGuildId(g).stream()
                    .anyMatch(c -> Objects.equals(c.getUserId(), user.getId()));
            boolean teacher = mentorshipRepository.findByGuildId(g).stream()
                    .anyMatch(m -> Objects.equals(m.getUserId(), user.getId()));
            if (!classmate && !teacher) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Out of scope"));
            }
            return ResponseEntity.ok(UserPublicDto.from(user));
        }).orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<?> create(User user) {
        if (user.getMail() == null || user.getMail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "mail is required"));
        }
        if (userRepository.findByMailIgnoreCase(user.getMail().trim()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "mail already exists"));
        }
        user.setId(null);
        user.setMail(user.getMail().trim());
        if (user.getHash() != null && !user.getHash().startsWith("$2")) {
            user.setHash(passwordEncoder.encode(user.getHash()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(userRepository.save(user));
    }

    public ResponseEntity<?> update(AuthUser me, Integer id, User data) {
        if (!me.isAdmin() && !Objects.equals(me.getId(), id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Can only update own profile"));
        }
        return userRepository.findById(id).<ResponseEntity<?>>map(user -> {
            if (data.getMail() != null) {
                if (!me.isAdmin()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Students/Teachers cannot change mail here"));
                }
                String mail = data.getMail().trim();
                if (mail.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("message", "mail is required"));
                }
                var existing = userRepository.findByMailIgnoreCase(mail);
                if (existing.isPresent() && !existing.get().getId().equals(id)) {
                    return ResponseEntity.badRequest().body(Map.of("message", "mail already exists"));
                }
                user.setMail(mail);
            }
            if (data.getName() != null) user.setName(data.getName());
            if (data.getHash() != null) {
                if (!data.getHash().startsWith("$2")) {
                    user.setHash(passwordEncoder.encode(data.getHash()));
                } else if (me.isAdmin()) {
                    user.setHash(data.getHash());
                }
            }
            if (data.getRole() != null) {
                if (!me.isAdmin()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Cannot change role"));
                }
                user.setRole(data.getRole());
            }
            return ResponseEntity.ok(userRepository.save(user));
        }).orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<Void> delete(Integer id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<?> login(LoginRequest request) {
        if (request.getMail() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "mail and password are required"));
        }

        return userRepository.findByMailIgnoreCase(request.getMail())
                .filter(user -> matchesPassword(request.getPassword(), user.getHash()))
                .<ResponseEntity<?>>map(user -> {
                    String token = jwtService.createToken(
                            user.getId(), user.getMail(), user.getName(), user.getRole().name());
                    Claims claims = jwtService.parse(token);
                    sessionActivityStore.register(claims.getId());
                    return ResponseEntity.ok(new LoginResponse(
                            user.getId(), user.getName(), user.getMail(), user.getRole(), token));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid credentials")));
    }

    private boolean matchesPassword(String rawPassword, String storedHash) {
        if (storedHash == null || rawPassword == null) {
            return false;
        }
        if (storedHash.startsWith("$2")) {
            return passwordEncoder.matches(rawPassword, storedHash);
        }
        return storedHash.equals(rawPassword);
    }
}
