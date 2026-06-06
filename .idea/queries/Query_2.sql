DROP TABLE IF EXISTS telegram_user_bot.mv_message_stats;

CREATE MATERIALIZED VIEW telegram_user_bot.mv_message_stats
            ENGINE = AggregatingMergeTree
                ORDER BY id
            POPULATE AS
SELECT
    id,
    anyLastState(title) AS last_title,
    countState() AS cnt_state
FROM telegram_user_bot.telegram_messages_new
GROUP BY id;
