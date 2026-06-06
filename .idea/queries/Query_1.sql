SELECT
    user_id,
    any(first_name)  AS first_name,
    any(second_name) AS second_name,
    any(username)    AS username,
    countIf(length(message) <= 50)  AS short_messages,
    countIf(length(message) > 50)   AS long_messages,
    count()                         AS total_messages
FROM chats_log
WHERE chat_id = -1001633660171
  AND toDate(date_time) = today()
GROUP BY user_id
ORDER BY total_messages DESC;
