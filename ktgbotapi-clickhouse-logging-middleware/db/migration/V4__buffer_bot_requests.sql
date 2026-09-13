-- A Buffer in front of `bot_requests`, so a logged API call is not its own part.
--
-- WHAT THIS IS FOR
--
-- The middleware writes one row per Telegram API call, each as its own INSERT,
-- and in ClickHouse one INSERT is one part. A Buffer table holds the rows in
-- server memory and writes them down together, so a burst of calls becomes one
-- part instead of one per call.
--
-- BE HONEST ABOUT THE SIZE OF THE WIN HERE
--
-- This table currently takes about 60 rows a day across all bots and holds 3
-- active parts. The merges have nothing to keep up with, and nothing about the
-- table is under pressure today. What this buys is the ceiling rather than the
-- present: the write pattern is one part per API call, and a bot that gets busy
-- -- or another bot added to the same table -- turns that into thousands of
-- parts a day without anything else changing. The Buffer makes the shape of the
-- write independent of the traffic.
--
-- WHY NO KOTLIN CHANGES
--
-- The Buffer takes over the NAME. `bot_requests` becomes the Buffer table and
-- the real MergeTree is renamed `bot_requests_data`, so
-- ClickHouseLoggingMiddleware goes on inserting into `bot_requests` exactly as
-- it does now, and the Grafana dashboard goes on querying it -- a SELECT on a
-- Buffer table reads the buffer and the table underneath both, so nothing it
-- shows even goes stale.
--
-- That layout is only available because nothing hangs off this table. A
-- materialized view is an AFTER INSERT trigger on its source table, so taking
-- the name away from the MergeTree would stop any view attached to it from
-- firing. `bot_requests` is the only table in this database and has no views on
-- it, so there is nothing to re-point.
--
-- WHAT IT COSTS
--
-- Up to a minute of API-call logs lives in ClickHouse's memory rather than on
-- disk, so a hard crash of the SERVER can lose them; an ordinary restart, a
-- DETACH or a DROP flushes cleanly. For a request log that is an acceptable
-- trade -- it is diagnostics, not data anything is reconstructed from.
--
-- A Buffer also has no index and is scanned in full on every read, and FINAL
-- and SAMPLE are not applied to buffered rows. Neither matters here: a minute
-- of this table is a handful of rows, and it is a plain MergeTree that nothing
-- queries with FINAL.

-- 1. The real table, exactly as it stands after V3. `bot_requests` still exists
--    and still serves every reader and writer while this runs.
CREATE TABLE IF NOT EXISTS bot_requests_data
(
    `timestamp` DateTime64(3) CODEC(Delta(8), ZSTD(9)),
    `bot` LowCardinality(String),
    `update_id` Int64 DEFAULT 0 CODEC(ZSTD(9)),
    `user_id` Int64 DEFAULT 0 CODEC(ZSTD(9)),
    `username` LowCardinality(String) DEFAULT '',
    `first_name` LowCardinality(String) DEFAULT '',
    `last_name` LowCardinality(String) DEFAULT '',
    `method` LowCardinality(String),
    `request` String CODEC(ZSTD(9)),
    `response` String CODEC(ZSTD(9)),
    `success` Bool,
    `error` String,
    `duration_ms` UInt32 CODEC(ZSTD(9))
)
ENGINE = MergeTree
ORDER BY (timestamp, bot, update_id)
SETTINGS index_granularity = 8192;

-- 2. The Buffer, still under a working name. Nothing has been renamed yet, so
--    if the server rejects this the deploy stops with the live table untouched.
--
--    Buffer(database, table, num_layers, min_time, max_time, min_rows, max_rows, min_bytes, max_bytes)
--    One layer: several bots write here, but a handful of rows a minute needs
--    no lock spreading. Flush at 60s / 10,000 rows / 10 MB, whichever comes
--    first. `request` and `response` are Telegram API JSON, so at any real
--    volume the byte ceiling is the one that fires.
CREATE TABLE IF NOT EXISTS bot_requests_buffer AS bot_requests_data
ENGINE = Buffer(
    currentDatabase(),
    bot_requests_data,
    1,
    10, 60,
    100, 10000,
    65536, 10485760
);

-- 3. The swap, in one atomic statement, and BEFORE the history is copied.
--
--    The order matters and is not the obvious one. Copying first and swapping
--    second leaves a gap the bots write into: a row landing in the old table
--    between the INSERT ... SELECT and the EXCHANGE is in neither the copy nor
--    the table that survives, and there is no transaction to close the window.
--    Swapping first closes it: after this statement the old MergeTree is named
--    `bot_requests_buffer`, nothing writes to it any more, and its contents are
--    final -- so the copy can neither miss a row nor duplicate one. (It would
--    duplicate: this is a plain MergeTree, with nothing to collapse the second
--    copy the way a ReplacingMergeTree would.)
--
--    The cost of this order is the other way round and much smaller: until step
--    4 finishes, `bot_requests` is a Buffer over an empty table, so the
--    dashboard sees only the last few seconds for as long as a ~140k-row copy
--    takes.
EXCHANGE TABLES bot_requests AND bot_requests_buffer;

-- 4. Move the history across, out of the old table which no longer receives
--    anything.
INSERT INTO bot_requests_data SELECT * FROM bot_requests_buffer;

-- 5. Drop it. Every row it held is in bot_requests_data, and from here on the
--    Buffer writes there.
DROP TABLE IF EXISTS bot_requests_buffer;
