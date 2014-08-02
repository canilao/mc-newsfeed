-- Table that records mcmmo level up events.
CREATE TABLE IF NOT EXISTS mcmmo_levelup_events (
    id INTEGER IDENTITY,
    event_uuid UUID,
    player_Id INT,
    time TIMESTAMP,
    skill_type VARCHAR(20),
    level INT
);

-- List of players table.
CREATE TABLE IF NOT EXISTS players (
    id INTEGER IDENTITY,
    name VARCHAR(16) UNIQUE
);

-- Login table.
CREATE TABLE IF NOT EXISTS logins (
    id BIGINT IDENTITY,
    player_Id INT,
    action VARCHAR(10),
    time TIMESTAMP
);

-- Player deaths table.
CREATE TABLE IF NOT EXISTS player_death_events (
    id INTEGER IDENTITY,
    event_uuid UUID,
    player_Id INT,
    time TIMESTAMP,
    killers_name VARCHAR(20),
    cause_of_death VARCHAR(30),
    death_message VARCHAR(100),
    weapon_used VARCHAR(40),
);

-- Block breaks table.
CREATE TABLE IF NOT EXISTS block_break_events (
    id INTEGER IDENTITY,
    event_uuid UUID,
    player_Id INT,
    time TIMESTAMP,
    blockType VARCHAR(40)
);

-- Create the login news table.
CREATE TABLE IF NOT EXISTS
    login_news (
        unique_id UUID,
        player_id INT,
        name VARCHAR(16),
        group_label INT,
        login_time TIMESTAMP,
        logout_time TIMESTAMP,
        last_action VARCHAR(10),
        play_time_minutes INT
);