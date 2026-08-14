package dam.guildmaster.service;

import dam.guildmaster.entity.GameCharacter;
import dam.guildmaster.entity.GameEvent;
import dam.guildmaster.entity.Skill;
import dam.guildmaster.enums.Aoe;
import dam.guildmaster.enums.EventStatus;
import dam.guildmaster.repository.CharacterRepository;
import dam.guildmaster.repository.EventRepository;
import dam.guildmaster.repository.PartyRepository;
import dam.guildmaster.repository.SkillRepository;
import dam.guildmaster.security.AccessService;
import dam.guildmaster.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final CharacterRepository characterRepository;
    private final SkillRepository skillRepository;
    private final PartyRepository partyRepository;
    private final AccessService accessService;

    public EventService(
            EventRepository eventRepository,
            CharacterRepository characterRepository,
            SkillRepository skillRepository,
            PartyRepository partyRepository,
            AccessService accessService) {
        this.eventRepository = eventRepository;
        this.characterRepository = characterRepository;
        this.skillRepository = skillRepository;
        this.partyRepository = partyRepository;
        this.accessService = accessService;
    }

    public List<GameEvent> findAll(AuthUser me, Integer guildId) {
        if (me.isAdmin()) {
            if (guildId != null) {
                return sortByCreatedDesc(eventRepository.findByGuildId(guildId));
            }
            return sortByCreatedDesc(eventRepository.findAll());
        }
        if (me.isTeacher()) {
            List<Integer> guilds = accessService.teacherGuildIds(me.getId());
            if (guildId != null) {
                accessService.requireTeacherGuild(guildId);
                return sortByCreatedDesc(eventRepository.findByGuildId(guildId));
            }
            return sortByCreatedDesc(eventRepository.findByGuildIdIn(guilds));
        }
        Integer g = accessService.requireStudentGuildContext(guildId);
        Set<Integer> myChars = accessService.myCharacterIdsInGuild(me, g);
        Set<Integer> myParties = accessService.myPartyIdsInGuild(me, g);
        return sortByCreatedDesc(eventRepository.findByGuildId(g).stream()
                .filter(ev -> studentCanSee(ev, myChars, myParties, g))
                .toList());
    }

    private List<GameEvent> sortByCreatedDesc(List<GameEvent> events) {
        return events.stream()
                .sorted(Comparator.comparing(GameEvent::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public ResponseEntity<?> findById(AuthUser me, Integer id, Integer guildId) {
        return eventRepository.findById(id).<ResponseEntity<?>>map(ev -> {
            if (me.isAdmin()) return ResponseEntity.ok(ev);
            if (me.isTeacher()) {
                accessService.requireTeacherGuild(ev.getGuildId());
                return ResponseEntity.ok(ev);
            }
            Integer g = accessService.requireStudentGuildContext(guildId != null ? guildId : ev.getGuildId());
            if (!Objects.equals(ev.getGuildId(), g)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Out of scope"));
            }
            Set<Integer> myChars = accessService.myCharacterIdsInGuild(me, g);
            Set<Integer> myParties = accessService.myPartyIdsInGuild(me, g);
            if (!studentCanSee(ev, myChars, myParties, g)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Out of scope"));
            }
            return ResponseEntity.ok(ev);
        }).orElse(ResponseEntity.notFound().build());
    }

    @Transactional
    public ResponseEntity<?> create(AuthUser me, GameEvent event) {
        if (me.isAdmin()) {
            return createAsAdmin(event);
        }
        if (me.isStudent() || me.isTeacher()) {
            if (me.isTeacher() && event.getSkillId() != null) {
                Skill maybeTeacherSkill = skillRepository.findById(event.getSkillId()).orElse(null);
                if (isTeacherExpSkill(maybeTeacherSkill)) {
                    return createTeacherExpEvent(me, event, maybeTeacherSkill);
                }
            }
            return createAsPlayer(me, event);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Forbidden"));
    }

    public ResponseEntity<?> update(AuthUser me, Integer id, GameEvent data) {
        return eventRepository.findById(id).<ResponseEntity<?>>map(event -> {
            EventStatus previousStatus = event.getStatus();
            Skill existingSkill = skillRepository.findById(event.getSkillId()).orElse(null);
            String changeJobTarget = isChangeJobSkill(existingSkill) ? event.getComment() : null;

            if (me.isAdmin()) {
                return updateAsAdmin(event, data, previousStatus, changeJobTarget);
            }
            if (me.isTeacher()) {
                return updateAsTeacher(me, event, data, previousStatus, changeJobTarget);
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Students cannot update events"));
        }).orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<?> delete(Integer id) {
        if (!eventRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        eventRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> createAsAdmin(GameEvent event) {
        event.setId(null);
        Skill skill = event.getSkillId() != null
                ? skillRepository.findById(event.getSkillId()).orElse(null)
                : null;
        coerceAutoEventFields(event, skill);
        clearReviewedAtIfNotReviewed(event);
        normalizeComment(event);
        ResponseEntity<?> invalid = validateCommentForStatus(event);
        if (invalid != null) return invalid;
        ResponseEntity<?> autoErr = validateAdminAutoShape(event, skill);
        if (autoErr != null) return autoErr;

        boolean skipCaster = Boolean.TRUE.equals(event.getSkipCasterExp());
        boolean skipTarget = Boolean.TRUE.equals(event.getSkipTargetExp());
        if (isAutoEventSkill(skill)) {
            skipCaster = false;
            skipTarget = false;
        }

        if (isTeacherExpSkill(skill)) {
            ResponseEntity<?> expErr = applyTeacherExpDelta(event, skill);
            if (expErr != null) return expErr;
        } else if (isLevelUpSkill(skill) || isChangeJobSkill(skill)) {
            ResponseEntity<?> effectErr = applyAdminAutoSkillEffects(event, skill, skipCaster);
            if (effectErr != null) return effectErr;
        } else if (skill != null) {
            GameCharacter caster = event.getCasterCharacterId() != null
                    ? characterRepository.findById(event.getCasterCharacterId()).orElse(null)
                    : null;
            ResponseEntity<?> expErr = chargeCasterExpCost(caster, skill, skipCaster);
            if (expErr != null) return expErr;
            if (event.getStatus() == EventStatus.APPROVED && !skipTarget) {
                ResponseEntity<?> giftErr = applyDebuffTargetGift(event, skill);
                if (giftErr != null) return giftErr;
            } else if (event.getStatus() == EventStatus.REJECTED && !skipCaster) {
                refundExp(event);
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(eventRepository.save(event));
    }

    private ResponseEntity<?> createAsPlayer(AuthUser me, GameEvent event) {
        ResponseEntity<?> err = validatePlayerEventCreate(me, event);
        if (err != null) return err;

        Skill skill = skillRepository.findById(event.getSkillId()).orElseThrow();
        GameCharacter caster = characterRepository.findById(event.getCasterCharacterId()).orElseThrow();

        event.setId(null);
        String changeJobRequest = null;
        if (isChangeJobSkill(skill)) {
            normalizeComment(event);
            changeJobRequest = event.getComment();
            String jobErr = validateChangeJobComment(changeJobRequest, caster.getJob());
            if (jobErr != null) {
                return ResponseEntity.badRequest().body(Map.of("message", jobErr));
            }
        } else {
            event.setComment(null);
        }

        if (isLevelUpSkill(skill)) {
            ResponseEntity<?> levelErr = applyLevelUpNow(caster, skill);
            if (levelErr != null) return levelErr;
        } else if (isChangeJobSkill(skill)) {
            ResponseEntity<?> jobErr = applyChangeJobNow(caster, changeJobRequest);
            if (jobErr != null) return jobErr;
        }

        ResponseEntity<?> expErr = chargeCasterExpCost(caster, skill, false);
        if (expErr != null) return expErr;

        if (isLevelUpSkill(skill)) {
            event.setStatus(EventStatus.AUTO);
            event.setComment("Auto-applied level up (no teacher review)");
            event.setReviewedByUserId(null);
            event.setReviewedAt(null);
        } else if (isChangeJobSkill(skill)) {
            String newJob = extractChangeJobTarget(changeJobRequest);
            event.setStatus(EventStatus.AUTO);
            event.setComment("Auto-applied Change Job to " + newJob);
            event.setReviewedByUserId(null);
            event.setReviewedAt(null);
        } else {
            event.setStatus(EventStatus.PENDING);
            event.setComment(null);
            event.setReviewedByUserId(null);
            event.setReviewedAt(null);
        }
        event.setCreatedAt(null);

        characterRepository.save(caster);
        GameEvent saved = eventRepository.save(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    private ResponseEntity<?> updateAsAdmin(
            GameEvent event,
            GameEvent data,
            EventStatus previousStatus,
            String changeJobTarget) {
        boolean fullEdit = data.getSkillId() != null;
        if (fullEdit) {
            event.setCasterCharacterId(data.getCasterCharacterId());
            event.setSkillId(data.getSkillId());
            if (data.getGuildId() != null) event.setGuildId(data.getGuildId());
            event.setTargetCharacterId(data.getTargetCharacterId());
            event.setTargetPartyId(data.getTargetPartyId());
        }
        if (data.getStatus() != null) event.setStatus(data.getStatus());
        event.setReviewedByUserId(data.getReviewedByUserId());
        event.setComment(data.getComment());
        if (data.getCreatedAt() != null) {
            event.setCreatedAt(data.getCreatedAt());
        }
        event.setReviewedAt(data.getReviewedAt());
        Skill skill = skillRepository.findById(event.getSkillId()).orElse(null);
        coerceAutoEventFields(event, skill);
        normalizeComment(event);
        ResponseEntity<?> invalid = validateCommentForStatus(event);
        if (invalid != null) return invalid;
        ResponseEntity<?> autoErr = validateAdminAutoShape(event, skill);
        if (autoErr != null) return autoErr;
        clearReviewedAtIfNotReviewed(event);
        markReviewedIfLeavingPending(previousStatus, event);
        ResponseEntity<?> effectErr = applyReviewSideEffects(previousStatus, event, changeJobTarget);
        if (effectErr != null) return effectErr;
        return ResponseEntity.ok(eventRepository.save(event));
    }

    private ResponseEntity<?> updateAsTeacher(
            AuthUser me,
            GameEvent event,
            GameEvent data,
            EventStatus previousStatus,
            String changeJobTarget) {
        accessService.requireTeacherGuild(event.getGuildId());
        if (data.getStatus() != null) event.setStatus(data.getStatus());
        event.setComment(data.getComment());
        normalizeComment(event);
        ResponseEntity<?> invalid = validateCommentForStatus(event);
        if (invalid != null) return invalid;
        if (event.getStatus() != null
                && (event.getStatus() == EventStatus.APPROVED || event.getStatus() == EventStatus.REJECTED)) {
            event.setReviewedByUserId(me.getId());
            if (previousStatus == EventStatus.PENDING || event.getReviewedAt() == null) {
                event.setReviewedAt(java.time.LocalDateTime.now());
            }
        }
        ResponseEntity<?> effectErr = applyReviewSideEffects(previousStatus, event, changeJobTarget);
        if (effectErr != null) return effectErr;
        return ResponseEntity.ok(eventRepository.save(event));
    }

    private ResponseEntity<?> createTeacherExpEvent(AuthUser me, GameEvent event, Skill skill) {
        if (event.getGuildId() == null || event.getSkillId() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "skill_id and guild_id are required"));
        }
        accessService.requireTeacherGuild(event.getGuildId());

        Integer amount = parsePositiveExpAmount(event.getComment());
        if (amount == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message",
                    "Comment must be a positive EXP amount (integer)"
            ));
        }

        boolean grant = isGrantExpSkill(skill);
        int delta = grant ? amount : -amount;
        event.setComment((delta > 0 ? "+" : "") + delta);

        ResponseEntity<?> expErr = applyTeacherExpDelta(event, skill);
        if (expErr != null) return expErr;

        event.setId(null);
        event.setCasterCharacterId(null);
        event.setStatus(EventStatus.AUTO);
        event.setReviewedByUserId(me.getId());
        event.setReviewedAt(null);
        event.setCreatedAt(null);
        GameEvent saved = eventRepository.save(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    private ResponseEntity<?> applyTeacherExpDelta(GameEvent event, Skill skill) {
        Integer amount = parsePositiveExpAmount(event.getComment());
        if (amount == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message",
                    "Comment must be a positive EXP amount (integer)"
            ));
        }
        if (event.getGuildId() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "guild_id is required"));
        }

        Integer tc = event.getTargetCharacterId();
        Integer tp = event.getTargetPartyId();
        List<GameCharacter> targets;
        if (tc != null && tp == null) {
            GameCharacter target = characterRepository.findById(tc).orElse(null);
            if (target == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Target character not found"));
            }
            if (!Objects.equals(target.getGuildId(), event.getGuildId())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Target character must be in selected guild"));
            }
            targets = List.of(target);
        } else if (tp != null && tc == null) {
            var party = partyRepository.findById(tp).orElse(null);
            if (party == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Target party not found"));
            }
            if (!Objects.equals(party.getGuildId(), event.getGuildId())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Target party must be in selected guild"));
            }
            targets = characterRepository.findByPartyId(tp);
            if (targets.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Target party has no characters"));
            }
        } else if (tc == null && tp == null) {
            targets = characterRepository.findByGuildId(event.getGuildId());
            if (targets.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Guild has no characters"));
            }
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "message",
                    "Set only target_character_id, only target_party_id, or neither for guild"
            ));
        }

        int delta;
        String raw = event.getComment() == null ? "" : event.getComment().trim();
        if (raw.startsWith("-")) {
            delta = -amount;
        } else if (raw.startsWith("+")) {
            delta = amount;
        } else {
            delta = isGrantExpSkill(skill) ? amount : -amount;
        }

        for (GameCharacter character : targets) {
            int current = character.getExp() == null ? 0 : character.getExp();
            int next = current + delta;
            if (next < 0) next = 0;
            character.setExp(next);
            characterRepository.save(character);
        }
        event.setComment((delta > 0 ? "+" : "-") + Math.abs(delta));
        return null;
    }

    private ResponseEntity<?> applyAdminAutoSkillEffects(GameEvent event, Skill skill, boolean skipCasterExp) {
        if (isTeacherExpSkill(skill)) {
            return applyTeacherExpDelta(event, skill);
        }
        if (!isLevelUpSkill(skill) && !isChangeJobSkill(skill)) {
            return null;
        }
        if (event.getCasterCharacterId() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Caster is required for Level Up / Change Job"));
        }
        GameCharacter caster = characterRepository.findById(event.getCasterCharacterId()).orElse(null);
        if (caster == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Caster character not found"));
        }
        int cost = skillExpCost(skill);
        int currentExp = caster.getExp() == null ? 0 : caster.getExp();
        if (!skipCasterExp && currentExp < cost) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message",
                    "Not enough EXP (need " + cost + ", have " + currentExp + ")"
            ));
        }
        if (isLevelUpSkill(skill)) {
            ResponseEntity<?> levelErr = applyLevelUpNow(caster, skill);
            if (levelErr != null) return levelErr;
        } else {
            ResponseEntity<?> jobErr = applyChangeJobNow(caster, event.getComment());
            if (jobErr != null) return jobErr;
        }
        if (!skipCasterExp) {
            caster.setExp(currentExp - cost);
        }
        characterRepository.save(caster);
        return null;
    }

    private ResponseEntity<?> chargeCasterExpCost(GameCharacter caster, Skill skill, boolean skipCasterExp) {
        if (skill == null || isTeacherExpSkill(skill)) {
            return null;
        }
        if (skipCasterExp) {
            return null;
        }
        int cost = skillExpCost(skill);
        if (caster == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message",
                    "Caster is required to spend skill ExpCost"
            ));
        }
        int current = caster.getExp() == null ? 0 : caster.getExp();
        if (current < cost) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message",
                    "Not enough EXP (need " + cost + ", have " + current + ")"
            ));
        }
        caster.setExp(current - cost);
        characterRepository.save(caster);
        return null;
    }

    private ResponseEntity<?> applyDebuffTargetGift(GameEvent event, Skill skill) {
        if (!isDebuffSkill(skill)) {
            return null;
        }
        int gift = skillExpCost(skill) / 2;
        if (gift <= 0) {
            return null;
        }
        List<GameCharacter> targets = resolveAoeTargets(event, skill);
        if (targets.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message",
                    "Debuff skill requires targets (character, party, or guild) to receive EXP"
            ));
        }
        for (GameCharacter target : targets) {
            int current = target.getExp() == null ? 0 : target.getExp();
            target.setExp(current + gift);
            characterRepository.save(target);
        }
        return null;
    }

    private List<GameCharacter> resolveAoeTargets(GameEvent event, Skill skill) {
        if (skill == null || skill.getAoe() == null) return List.of();
        Integer tc = event.getTargetCharacterId();
        Integer tp = event.getTargetPartyId();
        if (skill.getAoe() == Aoe.SINGLE) {
            if (tc == null) return List.of();
            return characterRepository.findById(tc).map(List::of).orElse(List.of());
        }
        if (skill.getAoe() == Aoe.PARTY) {
            if (tp == null) return List.of();
            return characterRepository.findByPartyId(tp);
        }
        if (skill.getAoe() == Aoe.GUILD) {
            if (event.getGuildId() == null) return List.of();
            return characterRepository.findByGuildId(event.getGuildId());
        }
        return List.of();
    }

    private static int skillExpCost(Skill skill) {
        return skill.getExpCost() == null ? 0 : skill.getExpCost();
    }

    private static boolean isDebuffSkill(Skill skill) {
        return skill != null && Boolean.TRUE.equals(skill.getDebuff());
    }

    private static boolean isAutoEventSkill(Skill skill) {
        return isTeacherExpSkill(skill) || isLevelUpSkill(skill) || isChangeJobSkill(skill);
    }

    private static void coerceAutoEventFields(GameEvent event, Skill skill) {
        if (isTeacherExpSkill(skill)) {
            event.setCasterCharacterId(null);
            event.setStatus(EventStatus.AUTO);
            event.setReviewedAt(null);
            Integer amount = parsePositiveExpAmount(event.getComment());
            if (amount != null) {
                int delta = isGrantExpSkill(skill) ? amount : -amount;
                event.setComment((delta > 0 ? "+" : "") + delta);
            }
            return;
        }
        if (isLevelUpSkill(skill)) {
            event.setStatus(EventStatus.AUTO);
            event.setReviewedByUserId(null);
            event.setReviewedAt(null);
            event.setComment("Auto-applied level up (no teacher review)");
            return;
        }
        if (isChangeJobSkill(skill)) {
            event.setStatus(EventStatus.AUTO);
            event.setReviewedByUserId(null);
            event.setReviewedAt(null);
            String job = extractChangeJobTarget(event.getComment());
            if (job != null) {
                event.setComment("Auto-applied Change Job to " + job);
            }
        }
    }

    private static void clearReviewedAtIfNotReviewed(GameEvent event) {
        EventStatus status = event.getStatus();
        if (status == EventStatus.PENDING || status == EventStatus.AUTO) {
            event.setReviewedAt(null);
        }
    }

    private static ResponseEntity<?> validateAdminAutoShape(GameEvent event, Skill skill) {
        if (isTeacherExpSkill(skill)) {
            if (event.getCasterCharacterId() != null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Teacher EXP events require empty Caster (NULL)"
                ));
            }
            if (event.getReviewedByUserId() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Teacher EXP events require Reviewed By (the teacher who cast)"
                ));
            }
            if (event.getStatus() != EventStatus.AUTO) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Teacher EXP events must have Status AUTO"
                ));
            }
            if (parsePositiveExpAmount(event.getComment()) == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Teacher EXP comment must be a signed positive amount (+N / -N)"
                ));
            }
            return null;
        }
        if (event.getCasterCharacterId() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Caster is required for this skill"
            ));
        }
        if (isLevelUpSkill(skill) || isChangeJobSkill(skill)) {
            if (event.getReviewedByUserId() != null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Level Up / Change Job must have empty Reviewed By"
                ));
            }
            if (event.getStatus() != EventStatus.AUTO) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Level Up / Change Job must have Status AUTO"
                ));
            }
            if (isLevelUpSkill(skill)
                    && !"Auto-applied level up (no teacher review)".equals(event.getComment())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Level Up comment must be: Auto-applied level up (no teacher review)"
                ));
            }
            if (isChangeJobSkill(skill) && extractChangeJobTarget(event.getComment()) == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Change Job requires target job (Mage, Rogue or Paladin)"
                ));
            }
            return null;
        }
        if (event.getStatus() == EventStatus.APPROVED || event.getStatus() == EventStatus.REJECTED) {
            if (event.getReviewedByUserId() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Reviewed By is required when Status is APPROVED or REJECTED"
                ));
            }
        }
        return null;
    }

    private static Integer parsePositiveExpAmount(String comment) {
        if (comment == null || comment.isBlank()) return null;
        String raw = comment.trim();
        if (raw.startsWith("+") || raw.startsWith("-")) {
            raw = raw.substring(1).trim();
        }
        try {
            int value = Integer.parseInt(raw);
            return value > 0 ? value : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean isTeacherExpSkill(Skill skill) {
        if (skill == null || skill.getJob() == null || !skill.getJob().equalsIgnoreCase("Teacher")) {
            return false;
        }
        return isGrantExpSkill(skill) || isRemoveExpSkill(skill);
    }

    private static boolean isGrantExpSkill(Skill skill) {
        if (skill.getName() == null) return false;
        String name = skill.getName().toLowerCase(Locale.ROOT);
        return name.contains("repartir exp") || name.contains("grant exp");
    }

    private static boolean isRemoveExpSkill(Skill skill) {
        if (skill.getName() == null) return false;
        String name = skill.getName().toLowerCase(Locale.ROOT);
        return name.contains("quitar exp") || name.contains("remove exp");
    }

    private ResponseEntity<?> validatePlayerEventCreate(AuthUser me, GameEvent event) {
        if (event.getCasterCharacterId() == null || event.getSkillId() == null || event.getGuildId() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "caster_character_id, skill_id and guild_id are required"));
        }
        GameCharacter caster = characterRepository.findById(event.getCasterCharacterId()).orElse(null);
        if (caster == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Caster character not found"));
        }
        if (me.isStudent() && !Objects.equals(caster.getUserId(), me.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Caster must be your character"));
        }
        if (me.isTeacher()) {
            accessService.requireTeacherGuild(caster.getGuildId());
        }
        if (!Objects.equals(event.getGuildId(), caster.getGuildId())) {
            return ResponseEntity.badRequest().body(Map.of("message", "guild_id must match caster character guild"));
        }
        Skill skill = skillRepository.findById(event.getSkillId()).orElse(null);
        if (skill == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Skill not found"));
        }
        if (skill.getJob() != null && skill.getJob().equalsIgnoreCase("Teacher")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Teacher skills only"));
        }
        if (!skillMatchesCasterJob(skill, caster)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Skill job must match caster job (or Common)"));
        }
        if (skill.getLevelReq() != null && caster.getLevel() != null && skill.getLevelReq() > caster.getLevel()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Caster level too low for this skill"));
        }
        if (isProgressionSkill(skill)) {
            event.setTargetCharacterId(caster.getId());
            event.setTargetPartyId(null);
            if (skill.getAoe() != Aoe.SINGLE) {
                return ResponseEntity.badRequest().body(Map.of("message", "Progression skills must be SINGLE"));
            }
        }
        String aoeErr = validateAoeTargets(skill.getAoe(), event, caster);
        if (aoeErr != null) {
            return ResponseEntity.badRequest().body(Map.of("message", aoeErr));
        }
        return null;
    }

    private ResponseEntity<?> applyReviewSideEffects(
            EventStatus previousStatus,
            GameEvent event,
            String preservedChangeJobTarget) {
        if (previousStatus != EventStatus.PENDING) {
            return null;
        }
        if (event.getStatus() == EventStatus.REJECTED) {
            refundExp(event);
            return null;
        }
        if (event.getStatus() == EventStatus.APPROVED) {
            Skill skill = skillRepository.findById(event.getSkillId()).orElse(null);
            ResponseEntity<?> giftErr = applyDebuffTargetGift(event, skill);
            if (giftErr != null) return giftErr;
            return applyApprovedProgression(event, preservedChangeJobTarget);
        }
        return null;
    }

    private void refundExp(GameEvent event) {
        Skill skill = skillRepository.findById(event.getSkillId()).orElse(null);
        if (skill == null || event.getCasterCharacterId() == null) return;
        GameCharacter caster = characterRepository.findById(event.getCasterCharacterId()).orElse(null);
        if (caster == null) return;
        int cost = skillExpCost(skill);
        int current = caster.getExp() == null ? 0 : caster.getExp();
        caster.setExp(current + cost);
        characterRepository.save(caster);
    }

    private ResponseEntity<?> applyApprovedProgression(GameEvent event, String preservedChangeJobTarget) {
        Skill skill = skillRepository.findById(event.getSkillId()).orElse(null);
        GameCharacter caster = characterRepository.findById(event.getCasterCharacterId()).orElse(null);
        if (skill == null || caster == null) return null;
        if (!isProgressionSkill(skill)) return null;

        Integer targetLevel = levelUpTargetLevel(skill);
        if (targetLevel != null) {
            int requiredCurrent = targetLevel - 1;
            if (caster.getLevel() == null || caster.getLevel() != requiredCurrent) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message",
                        "LEVEL UP TO LEVEL " + targetLevel + " requires character at level " + requiredCurrent
                ));
            }
            caster.setLevel(targetLevel);
            characterRepository.save(caster);
            return null;
        }
        if (isChangeJobSkill(skill)) {
            String newJob = extractChangeJobTarget(preservedChangeJobTarget);
            String jobErr = validateChangeJobComment(newJob, caster.getJob());
            if (jobErr != null) {
                return ResponseEntity.badRequest().body(Map.of("message", jobErr));
            }
            if (caster.getLevel() == null || caster.getLevel() < 3) {
                return ResponseEntity.badRequest().body(Map.of("message", "Change Job requires level 3+"));
            }
            caster.setJob(newJob);
            characterRepository.save(caster);
        }
        return null;
    }

    private ResponseEntity<?> applyLevelUpNow(GameCharacter caster, Skill skill) {
        Integer targetLevel = levelUpTargetLevel(skill);
        if (targetLevel == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Unknown Level Up skill"));
        }
        int requiredCurrent = targetLevel - 1;
        if (caster.getLevel() == null || caster.getLevel() != requiredCurrent) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message",
                    "LEVEL UP TO LEVEL " + targetLevel + " requires character at level " + requiredCurrent
            ));
        }
        caster.setLevel(targetLevel);
        return null;
    }

    private ResponseEntity<?> applyChangeJobNow(GameCharacter caster, String requestedJob) {
        String jobErr = validateChangeJobComment(requestedJob, caster.getJob());
        if (jobErr != null) {
            return ResponseEntity.badRequest().body(Map.of("message", jobErr));
        }
        if (caster.getLevel() == null || caster.getLevel() < 3) {
            return ResponseEntity.badRequest().body(Map.of("message", "Change Job requires level 3+"));
        }
        caster.setJob(extractChangeJobTarget(requestedJob));
        return null;
    }

    private static Integer levelUpTargetLevel(Skill skill) {
        if (!isLevelUpSkill(skill)) return null;
        String name = skill.getName() == null ? "" : skill.getName().toLowerCase(Locale.ROOT);
        if (name.contains("to level 4") || name.contains("level up iii")) return 4;
        if (name.contains("to level 3") || name.contains("level up ii")) return 3;
        if (name.contains("to level 2") || name.contains("level up i")) return 2;
        return null;
    }

    private static boolean isLevelUpSkill(Skill skill) {
        if (skill.getJob() == null || !skill.getJob().equalsIgnoreCase("Common")) return false;
        String name = skill.getName() == null ? "" : skill.getName().toLowerCase(Locale.ROOT);
        return name.contains("level up");
    }

    private static boolean skillMatchesCasterJob(Skill skill, GameCharacter caster) {
        if (skill.getJob() == null || caster.getJob() == null) return false;
        if (skill.getJob().equalsIgnoreCase("Common")) return true;
        return skill.getJob().equalsIgnoreCase(caster.getJob());
    }

    private static boolean isProgressionSkill(Skill skill) {
        if (skill.getJob() == null || !skill.getJob().equalsIgnoreCase("Common")) return false;
        String name = skill.getName() == null ? "" : skill.getName().toLowerCase(Locale.ROOT);
        return name.contains("level up") || name.contains("change job");
    }

    private static boolean isChangeJobSkill(Skill skill) {
        if (skill.getName() == null) return false;
        return skill.getName().toLowerCase(Locale.ROOT).contains("change job");
    }

    private static String validateChangeJobComment(String comment, String currentJob) {
        String job = extractChangeJobTarget(comment);
        if (job == null) {
            return "Change Job requires comment with target job (Mage, Rogue or Paladin)";
        }
        if (currentJob != null && job.equalsIgnoreCase(currentJob)) {
            return "Change Job target must be a different class";
        }
        return null;
    }

    private static String extractChangeJobTarget(String comment) {
        if (comment == null || comment.isBlank()) return null;
        String raw = comment.trim();
        String prefix = "Auto-applied Change Job to ";
        if (raw.regionMatches(true, 0, prefix, 0, prefix.length())) {
            raw = raw.substring(prefix.length()).trim();
        }
        if (!raw.equalsIgnoreCase("Mage") && !raw.equalsIgnoreCase("Rogue") && !raw.equalsIgnoreCase("Paladin")) {
            return null;
        }
        return capitalizeJob(raw);
    }

    private static String capitalizeJob(String job) {
        String j = job.trim().toLowerCase(Locale.ROOT);
        return j.substring(0, 1).toUpperCase(Locale.ROOT) + j.substring(1);
    }

    private String validateAoeTargets(Aoe aoe, GameEvent event, GameCharacter caster) {
        Integer tc = event.getTargetCharacterId();
        Integer tp = event.getTargetPartyId();
        if (aoe == Aoe.SINGLE) {
            if (tc == null) return "SINGLE skills require target_character_id";
            if (tp != null) return "SINGLE skills must not set target_party_id";
            GameCharacter target = characterRepository.findById(tc).orElse(null);
            if (target == null) return "Target character not found";
            if (!Objects.equals(target.getGuildId(), caster.getGuildId())) {
                return "Target character must be in the same guild";
            }
            return null;
        }
        if (aoe == Aoe.PARTY) {
            if (tp == null) return "PARTY skills require target_party_id";
            if (tc != null) return "PARTY skills must not set target_character_id";
            var party = partyRepository.findById(tp).orElse(null);
            if (party == null) return "Target party not found";
            if (!Objects.equals(party.getGuildId(), caster.getGuildId())) {
                return "Target party must be in the same guild";
            }
            return null;
        }
        if (aoe == Aoe.GUILD) {
            if (tc != null || tp != null) {
                return "GUILD skills must have target_character_id and target_party_id null";
            }
            return null;
        }
        return "Unknown AOE";
    }

    private boolean studentCanSee(GameEvent ev, Set<Integer> myChars, Set<Integer> myParties, Integer guildId) {
        if (ev.getCasterCharacterId() != null && myChars.contains(ev.getCasterCharacterId())) return true;
        if (ev.getTargetCharacterId() != null && myChars.contains(ev.getTargetCharacterId())) return true;
        if (ev.getTargetPartyId() != null && myParties.contains(ev.getTargetPartyId())) return true;
        if (ev.getTargetCharacterId() == null && ev.getTargetPartyId() == null
                && Objects.equals(ev.getGuildId(), guildId)) {
            Skill skill = skillRepository.findById(ev.getSkillId()).orElse(null);
            return skill != null && skill.getAoe() == Aoe.GUILD;
        }
        return false;
    }

    private static void markReviewedIfLeavingPending(EventStatus previousStatus, GameEvent event) {
        if (previousStatus == EventStatus.PENDING
                && event.getStatus() != null
                && (event.getStatus() == EventStatus.APPROVED || event.getStatus() == EventStatus.REJECTED)
                && event.getReviewedAt() == null) {
            event.setReviewedAt(java.time.LocalDateTime.now());
        }
    }

    private static void normalizeComment(GameEvent event) {
        String comment = event.getComment();
        if (comment != null && comment.isBlank()) {
            event.setComment(null);
        }
    }

    private static ResponseEntity<Map<String, String>> validateCommentForStatus(GameEvent event) {
        EventStatus status = event.getStatus();
        if (status != null && status != EventStatus.PENDING) {
            String comment = event.getComment();
            if (comment == null || comment.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Comment is required when Status is APPROVED, REJECTED or AUTO"));
            }
        }
        return null;
    }
}
