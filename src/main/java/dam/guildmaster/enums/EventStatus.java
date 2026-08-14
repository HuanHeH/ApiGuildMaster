package dam.guildmaster.enums;

public enum EventStatus {
    PENDING,
    APPROVED,
    REJECTED,
    /** Auto-resolved: Level Up, Change Job, Teacher Grant/Remove EXP (no teacher review date). */
    AUTO
}
