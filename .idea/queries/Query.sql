CREATE TABLE bot_interactions
(
    date DateTime,
    user_id Int64,
    username String,
    first_name String,
    last_name String,
    interactions UInt32,
    app_name LowCardinality(String)
)
    ENGINE = SummingMergeTree
        ORDER BY (toDate(date), user_id, app_name)
        SETTINGS index_granularity = 8192;