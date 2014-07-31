SET @group_label = 0;
SET @player_id = %d;
SET @time_threshold = %d;
SET @unit_of_time = '%s';

-- Holds the last logout of the player
SET @last_logout= CAST('2000-01-01 00:00:00' AS TIMESTAMP);

-- Create our temp tables
CREATE TEMPORARY TABLE IF NOT EXISTS
    grouped_rows (
        seq INT,
        id INT,
        player_id INT,
        action VARCHAR(10),
        time TIMESTAMP,
        lag_seq INT,
        lag_time TIMESTAMP,
        group_label INT,
        time_delay INT,
        name VARCHAR(16),
    );

-- Get the last logout in the login_news table
SET @last_logout = SELECT COALESCE(MAX(logout_time), @last_logout) FROM login_news WHERE @player_id = player_id;

-- Group up the rows according to the threshold
INSERT INTO grouped_rows
    SELECT c.*, d.name 
    FROM (
        SELECT a.*, b.seq AS lag_seq, b.time AS lag_time,
            (CASE 
                WHEN (TIMESTAMPDIFF(@unit_of_time, b.time, a.time)) > @time_threshold  AND a.action != 'logout'
                THEN @group_label := @group_label + 1
                ELSE @group_label
            END) group_Label,
            TIMESTAMPDIFF(@unit_of_time, b.time, a.time) AS time_delay
        FROM (
            SELECT ROWNUM() AS seq, * FROM logins WHERE player_id=@player_id 
        ) a 
        LEFT JOIN (
            SELECT ROWNUM() AS seq, * FROM logins WHERE player_id=@player_id 
        ) b 
        ON a.seq= b.seq + 1
    ) c 
    JOIN PLAYERS d on c.player_id=d.id;

-- Get the login and logout time with the time played and put the new rows in the news table.
MERGE INTO login_news KEY(login_time, name) (
    SELECT *
    FROM (
        SELECT 
            a.player_id,
            a.name, 
            a.group_label, 
            MIN(time) AS login_time, 
            b.logout_time,
            (SELECT action FROM grouped_rows WHERE grouped_rows.group_label= a.group_label ORDER BY time DESC LIMIT 1) as last_action,
            TIMESTAMPDIFF('MINUTE', MIN(time), b.logout_time) as play_time_minutes
        FROM grouped_rows a
        JOIN (
            SELECT group_label, MAX(time) AS logout_time
            FROM grouped_rows c
            GROUP BY c.group_label
        ) b ON a.group_label=b.group_label 
        GROUP BY a.group_label
        ORDER BY a.group_label
    )
);

-- Drop our temp table
DROP TABLE grouped_rows;

