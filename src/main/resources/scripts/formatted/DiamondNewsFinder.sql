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
        block_type VARCHAR(40),
        lag_seq INT,
        lag_time TIMESTAMP,
        group_label INT,
        time_delay INT,
        name VARCHAR(16)
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
            SELECT ROWNUM() AS seq, * FROM block_break_events WHERE player_id=@player_id 
        ) a 
        LEFT JOIN (
            SELECT ROWNUM() AS seq, * FROM block_break_events WHERE player_id=@player_id 
        ) b 
        ON a.seq= b.seq + 1
    ) c 
    JOIN PLAYERS d on c.player_id=d.id;

-- Get the the diamond break news and place it in the table 
MERGE INTO diamond_break_news (
    event_uuid,
    block_count,
    player_id,
    name,
    time,
    block_type,
    group_label
) KEY(event_uuid) (
    SELECT 
        (SELECT event_uuid FROM grouped_rows c WHERE b.time=c.time AND c.player_id=@player_id) AS event_uuid, 
        * 
    FROM (
        SELECT 
            COUNT(*) AS block_count, 
            a.player_id, 
            a.name, 
            MIN(a.time) AS time, 
            block_type, 
            a.group_label 
        FROM 
            grouped_rows a 
        GROUP BY 
            group_label
    ) b
);

-- Drop our temp table
DROP TABLE grouped_rows;


