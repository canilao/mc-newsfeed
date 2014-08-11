SET @group_label = 0;
SET @player_id = %d;
SET @time_threshold = %d;
SET @unit_of_time = '%s';

-- Create our temp tables
CREATE TEMPORARY TABLE IF NOT EXISTS
    grouped_rows (
        seq INT,
        id INT,
        event_uuid UUID,
        player_id INT,
        time TIMESTAMP,
        killers_name VARCHAR(20),
        cause_of_death VARCHAR(30),
        death_message VARCHAR(100),
        weapon_used VARCHAR(40),
        lag_seq INT,
        lag_time TIMESTAMP,
        group_label INT,
        time_delay INT,
        name VARCHAR(20)
    );

-- Group up the rows according to the threshold
INSERT INTO grouped_rows
    SELECT c.*, d.name 
    FROM (
        SELECT a.*, b.seq AS lag_seq, b.time AS lag_time,
            (CASE 
                WHEN (TIMESTAMPDIFF(@unit_of_time, b.time, a.time)) > @time_threshold
                THEN @group_label := @group_label + 1
                ELSE @group_label
            END) group_Label,
            TIMESTAMPDIFF(@unit_of_time, b.time, a.time) AS time_delay
        FROM (
            SELECT ROWNUM() AS seq, * FROM player_death_events WHERE player_id=@player_id 
        ) a 
        LEFT JOIN (
            SELECT ROWNUM() AS seq, * FROM player_death_events WHERE player_id=@player_id 
        ) b 
        ON a.seq= b.seq + 1
    ) c 
    JOIN PLAYERS d on c.player_id=d.id;

INSERT INTO death_groups (death_event_uuid, group_label, player_id)
SELECT DISTINCT event_uuid, group_label, player_id
FROM grouped_rows a
WHERE NOT EXISTS (
    SELECT * 
    FROM death_groups b
    WHERE a.event_uuid = b.death_event_uuid);

-- Get the the player death news and place it in the table 
MERGE INTO player_death_news (
    event_uuid,
    death_count,
    player_id,
    name,
    time,
    group_label
) KEY(event_uuid) (
    SELECT 
        (SELECT event_uuid FROM grouped_rows c WHERE b.time=c.time AND c.player_id=@player_id ORDER BY id DESC LIMIT 1) AS event_uuid, 
        * 
    FROM (
        SELECT 
            (SELECT COUNT(*) FROM death_groups WHERE group_label=a.group_label AND player_id=@player_id) AS death_count, 
            a.player_id, 
            a.name, 
            MIN(a.time) AS time, 
            a.group_label 
        FROM 
            grouped_rows a 
        GROUP BY 
            group_label
    ) b
);

-- Drop our temp table
DROP TABLE grouped_rows;
