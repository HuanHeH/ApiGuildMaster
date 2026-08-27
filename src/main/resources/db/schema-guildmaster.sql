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
    `Job`     VARCHAR(50) NOT NULL,
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