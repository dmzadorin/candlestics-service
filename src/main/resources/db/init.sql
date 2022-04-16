CREATE TABLE IF NOT EXISTS instruments
(
    isin        TEXT UNIQUE,
    description TEXT
);

CREATE TABLE IF NOT EXISTS quotes
(
    id    serial,
    ts    TIMESTAMPTZ,
    isin  TEXT,
    price DOUBLE PRECISION
);

SELECT create_hypertable('quotes', 'ts');

CREATE MATERIALIZED VIEW IF NOT EXISTS one_min_candle
    WITH (timescaledb.continuous) AS
SELECT time_bucket('1 min', ts) AS bucket,
       isin,
       FIRST(price, ts)         AS "open",
       MAX(price)               AS high,
       MIN(price)               AS low,
       LAST(price, ts)          AS "close"
FROM quotes
GROUP BY bucket, isin;

SELECT add_continuous_aggregate_policy(
               'one_min_candle',
               start_offset => INTERVAL '1 hour',
               end_offset => INTERVAL '10 sec',
               schedule_interval => INTERVAL '1 min'
           );
