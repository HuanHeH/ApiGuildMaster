package dam.guildmaster.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "Mentorships")
@IdClass(Mentorship.MentorshipId.class)
public class Mentorship {

    @Id
    @Column(name = "UserID")
    private Integer userId;

    @Id
    @Column(name = "GuildID")
    private Integer guildId;

    public Mentorship() {}

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getGuildId() { return guildId; }
    public void setGuildId(Integer guildId) { this.guildId = guildId; }

    public static class MentorshipId implements Serializable {
        private Integer userId;
        private Integer guildId;

        public MentorshipId() {}

        public MentorshipId(Integer userId, Integer guildId) {
            this.userId = userId;
            this.guildId = guildId;
        }

        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }

        public Integer getGuildId() { return guildId; }
        public void setGuildId(Integer guildId) { this.guildId = guildId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MentorshipId that)) return false;
            return Objects.equals(userId, that.userId) && Objects.equals(guildId, that.guildId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, guildId);
        }
    }
}
