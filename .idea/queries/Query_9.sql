-- 1. Создаём временную таблицу с нужным типом поля
CREATE TABLE telegram_messages_new_tmp
(
    date_time  DateTime,
    message    String,
    title      LowCardinality(String),
    id         Int64,
    admins2    Array(LowCardinality(String)),
    usernames  Array(LowCardinality(String)),
    message_id UInt64,
    reply_to   UInt64 DEFAULT 0,  -- заменили Nullable
    raw        String
)
    ENGINE = MergeTree
        ORDER BY date_time
        SETTINGS index_granularity = 8192;

-- 2. Копируем данные, заменяя NULL на 0
INSERT INTO telegram_messages_new_tmp
SELECT
    date_time,
    message,
    title,
    id,
    admins2,
    usernames,
    message_id,
    ifNull(reply_to, 0),
    raw
FROM telegram_messages_new;

-- 3. Переименовываем таблицы
RENAME TABLE telegram_messages_new TO telegram_messages_new_backup,   -- сохраняем оригинал
    telegram_messages_new_tmp TO telegram_messages_new;     -- новая — под старым именем
