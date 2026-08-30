-- ============================================================
-- GuildMaster - Esquema MariaDB
-- Importa este archivo en la base de datos `GuildMaster`
-- (phpMyAdmin: Import | o CLI: mariadb GuildMaster < schema-guildmaster.sql)
--
-- Nota: spring.jpa.hibernate.ddl-auto=none, por lo que estas
-- tablas deben existir ANTES de arrancar la API.
-- ============================================================

-- ---------- Users ----------
CREATE TABLE IF NOT EXISTS `Users` (
    `ID`   INT AUTO_INCREMENT PRIMARY KEY,
    `Name` VARCHAR(100) NOT NULL,
    `Mail` VARCHAR(150) NOT NULL UNIQUE,
    `Hash` VARCHAR(255) NOT NULL,
    `Role` VARCHAR(20)  NOT NULL,
    CONSTRAINT `chk_Users_Role` CHECK (`Role` IN ('Student', 'Teacher', 'Admin'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- Guilds ----------
CREATE TABLE IF NOT EXISTS `Guilds` (
    `ID`       INT AUTO_INCREMENT PRIMARY KEY,
    `Name`     VARCHAR(100) NOT NULL UNIQUE,
    `Number`   INT NOT NULL,
    `Letter`   VARCHAR(1) NOT NULL,
    `Level`    VARCHAR(50) NULL,
    `Modality` VARCHAR(50) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- Mentorships ----------
CREATE TABLE IF NOT EXISTS `Mentorships` (
    `UserID`  INT NOT NULL,
    `GuildID` INT NOT NULL,
    PRIMARY KEY (`UserID`, `GuildID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- Parties ----------
CREATE TABLE IF NOT EXISTS `Parties` (
    `ID`      INT AUTO_INCREMENT PRIMARY KEY,
    `Name`    VARCHAR(100) NOT NULL UNIQUE,
    `GuildID` INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- Characters ----------
CREATE TABLE IF NOT EXISTS `Characters` (
    `ID`      INT AUTO_INCREMENT PRIMARY KEY,
    `Name`    VARCHAR(100) NOT NULL,
    `Job`     VARCHAR(50) NULL,
    `Level`   INT NOT NULL DEFAULT 1,
    `Exp`     INT NOT NULL DEFAULT 0,
    `UserID`  INT NOT NULL,
    `GuildID` INT NOT NULL,
    `PartyID` INT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- Skills ----------
CREATE TABLE IF NOT EXISTS `Skills` (
    `ID`          INT AUTO_INCREMENT PRIMARY KEY,
    `Name`        VARCHAR(100) NOT NULL,
    `LevelReq`    INT NOT NULL,
    `Job`         VARCHAR(50) NOT NULL,
    `Description` TEXT NULL,
    `AOE`         VARCHAR(20) NOT NULL,
    `ExpCost`     INT NOT NULL,
    `Debuff`      TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT `chk_Skills_AOE` CHECK (`AOE` IN ('SINGLE', 'PARTY', 'GUILD'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- Events ----------
CREATE TABLE IF NOT EXISTS `Events` (
    `ID`                INT AUTO_INCREMENT PRIMARY KEY,
    `CasterCharacterID` INT NULL,
    `SkillID`           INT NOT NULL,
    `GuildID`           INT NOT NULL,
    `TargetCharacterID` INT NULL,
    `TargetPartyID`     INT NULL,
    `Status`            VARCHAR(20) NOT NULL,
    `ReviewedByUserID`  INT NULL,
    `CreatedAt`         DATETIME NOT NULL,
    `ReviewedAt`        DATETIME NULL,
    `Comment`           TEXT NULL,
    CONSTRAINT `chk_Events_Status` CHECK (`Status` IN ('PENDING', 'APPROVED', 'REJECTED', 'AUTO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- Foreign keys ----------
-- Delete character  -> events where caster/target
-- Delete user       -> characters (then their events) + mentorships
-- Delete guild      -> parties, characters, mentorships, events
-- Delete party      -> events targeting that party; character.PartyID -> NULL
-- Delete user (reviewer) -> Events.ReviewedByUserID -> NULL

ALTER TABLE `Mentorships`
    ADD CONSTRAINT `FK_Mentorship_User`
        FOREIGN KEY (`UserID`) REFERENCES `Users` (`ID`) ON DELETE CASCADE,
    ADD CONSTRAINT `FK_Mentorship_Guild`
        FOREIGN KEY (`GuildID`) REFERENCES `Guilds` (`ID`) ON DELETE CASCADE;

ALTER TABLE `Parties`
    ADD CONSTRAINT `FK_Party_Guild`
        FOREIGN KEY (`GuildID`) REFERENCES `Guilds` (`ID`) ON DELETE CASCADE;

ALTER TABLE `Characters`
    ADD CONSTRAINT `FK_Character_User`
        FOREIGN KEY (`UserID`) REFERENCES `Users` (`ID`) ON DELETE CASCADE,
    ADD CONSTRAINT `FK_Character_Guild`
        FOREIGN KEY (`GuildID`) REFERENCES `Guilds` (`ID`) ON DELETE CASCADE,
    ADD CONSTRAINT `FK_Character_Party`
        FOREIGN KEY (`PartyID`) REFERENCES `Parties` (`ID`) ON DELETE SET NULL;

ALTER TABLE `Events`
    ADD CONSTRAINT `FK_Event_Caster`
        FOREIGN KEY (`CasterCharacterID`) REFERENCES `Characters` (`ID`) ON DELETE CASCADE,
    ADD CONSTRAINT `FK_Event_TargetCharacter`
        FOREIGN KEY (`TargetCharacterID`) REFERENCES `Characters` (`ID`) ON DELETE CASCADE,
    ADD CONSTRAINT `FK_Event_Guild`
        FOREIGN KEY (`GuildID`) REFERENCES `Guilds` (`ID`) ON DELETE CASCADE,
    ADD CONSTRAINT `FK_Event_TargetParty`
        FOREIGN KEY (`TargetPartyID`) REFERENCES `Parties` (`ID`) ON DELETE CASCADE,
    ADD CONSTRAINT `FK_Event_Reviewer`
        FOREIGN KEY (`ReviewedByUserID`) REFERENCES `Users` (`ID`) ON DELETE SET NULL,
    ADD CONSTRAINT `FK_Event_Skill`
        FOREIGN KEY (`SkillID`) REFERENCES `Skills` (`ID`);
