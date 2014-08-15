MERGE INTO all_news KEY(event_uuid) (
    SELECT ROWNUM() AS id, * FROM (
        SELECT * FROM (
            SELECT
                'death' AS type, 
                event_uuid,
                time
            FROM player_death_news

            UNION

            SELECT 
                'login' AS type, 
                event_uuid,
                login_time AS time
            FROM login_news 

            UNION

            SELECT 
                'mcmmo_levelup' AS type, 
                event_uuid,
                time
            FROM mcmmo_levelup_events 

            UNION

            SELECT 
                'diamond_break' AS type, 
                event_uuid,
                time
            FROM diamond_break_news 

            UNION

            SELECT 
                'achievement' AS type, 
                event_uuid,
                time
            FROM achievement_events 
        )
        ORDER BY time DESC
    )
); 

