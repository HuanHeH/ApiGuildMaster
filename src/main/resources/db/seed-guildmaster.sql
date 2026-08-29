-- ============================================================
-- GuildMaster - Datos de ejemplo (SEED)
-- Ejecuta DESPUÉS de schema-guildmaster.sql.
-- Crea usuarios de prueba para cada rol y datos básicos.
--
-- IMPORTANTE sobre contraseñas:
--  * La API detecta hashes que NO empiezan por "$2" y los compara
--    en texto plano; al primer login correcto los convierte a BCrypt.
--  * Las contraseñas de ejemplo son: admin123 / teacher123 / student123
-- ============================================================

-- ---------- Usuarios de prueba ----------
INSERT INTO `Users` (`Name`, `Mail`, `Hash`, `Role`) VALUES
    ('Admin Demo',   'admin@example.com',   'admin123',   'Admin'),
    ('Teacher Demo', 'teacher@example.com', 'teacher123', 'Teacher'),
    ('Student Demo', 'student@example.com', 'student123', 'Student');

-- ---------- Una guild de ejemplo ----------
INSERT INTO `Guilds` (`Name`, `Number`, `Letter`, `Level`, `Modality`) VALUES
    ('Demo Guild', 1, 'A', 'ESO', 'DAM');

-- ---------- Mentorship: teacher -> guild ----------
INSERT INTO `Mentorships` (`UserID`, `GuildID`) VALUES
    (2, 1);

-- ---------- Un personaje para el student ----------
INSERT INTO `Characters` (`Name`, `Job`, `Level`, `Exp`, `UserID`, `GuildID`, `PartyID`) VALUES
    ('Demo Mage', 'Mage', 1, 100, 3, 1, NULL);

-- ---------- Skills de ejemplo ----------
INSERT INTO `Skills` (`Name`, `LevelReq`, `Job`, `Description`, `AOE`, `ExpCost`, `Debuff`) VALUES
    ('Level Up I',        1, 'Common',  'Level up to level 2.',          'SINGLE', 50, 0),
    ('Level Up II',       2, 'Common',  'Level up to level 3.',          'SINGLE', 90, 0),
    ('Change Job',        3, 'Common',  'Change class (Mage/Rogue/Paladin).', 'SINGLE', 80, 0),
    ('Fireball',          1, 'Mage',    'Deal damage to a single target.', 'SINGLE', 30, 1),
    ('Repartir EXP',      1, 'Teacher', 'Grant EXP to a target.',         'SINGLE', 0,  0),
    ('Quitar EXP',        1, 'Teacher', 'Remove EXP from a target.',       'SINGLE', 0,  0),
    ('Choose Class',      0, 'Common',  'Choose your class: Mage (spellcaster), Rogue (stealthy), or Paladin (tank). Available only for characters without a class.', 'SINGLE', 0, 0);