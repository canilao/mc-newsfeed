SELECT * 
FROM all_news
WHERE id > (SELECT id FROM all_news WHERE event_uuid='%s') 
AND id < (((SELECT id FROM all_news WHERE event_uuid='%s')) + %d + 1);
