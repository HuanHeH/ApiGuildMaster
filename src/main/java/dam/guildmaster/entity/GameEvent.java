package dam.guildmaster.entity;

import dam.guildmaster.enums.EventStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Events")
public class GameEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "CasterCharacterID", nullable = false)
    private Integer casterCharacterId;

    @Column(name = "SkillID", nullable = false)
    private Integer skillId;

    @Column(name = "GuildID", nullable = false)
    private Integer guildId;

    @Column(name = "TargetCharacterID")
    private Integer targetCharacterId;

    @Column(name = "TargetPartyID")
    private Integer targetPartyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    private EventStatus status = EventStatus.PENDING;

    @Column(name = "ReviewedByUserID")
    private Integer reviewedByUserId;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "Comment", columnDefinition = "TEXT")
    private String comment;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = EventStatus.PENDING;
        }
    }

    public GameEvent() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getCasterCharacterId() { return casterCharacterId; }
    public void setCasterCharacterId(Integer casterCharacterId) { this.casterCharacterId = casterCharacterId; }

    public Integer getSkillId() { return skillId; }
    public void setSkillId(Integer skillId) { this.skillId = skillId; }

    public Integer getGuildId() { return guildId; }
    public void setGuildId(Integer guildId) { this.guildId = guildId; }

    public Integer getTargetCharacterId() { return targetCharacterId; }
    public void setTargetCharacterId(Integer targetCharacterId) { this.targetCharacterId = targetCharacterId; }

    public Integer getTargetPartyId() { return targetPartyId; }
    public void setTargetPartyId(Integer targetPartyId) { this.targetPartyId = targetPartyId; }

    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }

    public Integer getReviewedByUserId() { return reviewedByUserId; }
    public void setReviewedByUserId(Integer reviewedByUserId) { this.reviewedByUserId = reviewedByUserId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
