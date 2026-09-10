-- Codecs for `bot_requests`: ZSTD(9) for the payloads, Delta for the timestamp.
--
-- The table has never had a codec, so everything is on ClickHouse's default
-- LZ4. It is 4.69 MiB for 140k rows, and nearly all of it is two columns of
-- Telegram API JSON -- 108 MB of `response` and 18 MB of `request`
-- uncompressed.
--
-- Measured over each column as it stands, read in the table's sort order
-- (`ORDER BY timestamp, bot, update_id`), compressed in the 1 MiB blocks
-- ClickHouse uses. `now` is the column's size on disk under LZ4:
--
--                    now      ZSTD(9)   Delta+ZSTD(9)
--     response     2738400     934823        --
--     request       986986     365201        --
--     timestamp     708669     322946      213715
--     duration_ms   353628     125607      145091
--     update_id      36078      18545       24638
--     user_id        18784       6242        6840
--
-- Together: 4.84 MB today, 1.66 MB -- the table goes to roughly 1.7 MiB.
--
-- The JSON is where most of it comes from. Every row is the same handful of
-- Telegram object shapes with different values in them, so the same keys
-- appear thousands of times over: `response` is at ratio 40 under LZ4 already
-- and still nearly triples with ZSTD, because a 1 MiB window sees repetition
-- across rows that LZ4's window is too small to reach.
--
-- `timestamp` is the one column that wants Delta: rows are stored in time
-- order, so consecutive values differ by milliseconds, and the deltas are
-- small integers. It is `DateTime64(3)`, eight bytes wide, hence `Delta(8)`.
--
-- Delta is deliberately *not* used on the other three. `duration_ms`,
-- `update_id` and `user_id` all come out worse with it (`duration_ms` 145091
-- against 125607): durations are noise around a few common values and
-- `user_id` repeats the same few people, so what compresses them is the
-- recurrence of whole values -- which Delta replaces with differences that
-- recur far less.
--
-- ZSTD(9) rather than a lower level: above level 1 zstd's decompression speed
-- barely moves, so the level is paid for in insert CPU only, and the
-- middleware writes one small row per API call.
--
-- Not touched: `bot`, `username`, `first_name`, `last_name` and `method` are
-- `LowCardinality(String)`, whose dictionary already does the deduplication a
-- codec would be looking for, and `success` and `error` are a few KB.
--
-- MODIFY COLUMN changes metadata only: existing parts keep their LZ4 bytes
-- until something rewrites them, and OPTIMIZE FINAL is that something. The
-- table is partitioned by day, so that is one pass over each day's part.
--
-- Nothing in the middleware changes -- a codec is how a column is stored, not
-- what it holds -- and the types are not restated below, so the DEFAULTs
-- declared in V1 survive.

ALTER TABLE bot_requests
    MODIFY COLUMN response    CODEC(ZSTD(9)),
    MODIFY COLUMN request     CODEC(ZSTD(9)),
    MODIFY COLUMN timestamp   CODEC(Delta(8), ZSTD(9)),
    MODIFY COLUMN duration_ms CODEC(ZSTD(9)),
    MODIFY COLUMN update_id   CODEC(ZSTD(9)),
    MODIFY COLUMN user_id     CODEC(ZSTD(9));

OPTIMIZE TABLE bot_requests FINAL;
