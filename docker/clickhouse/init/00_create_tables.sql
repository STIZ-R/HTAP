-- -------------------------
-- USERS
-- -------------------------
CREATE TABLE IF NOT EXISTS users (
    id UInt32,
    name String,
    email String,

    _op String,           -- Debezium operation: c,u,d
    _version UInt64,      -- Debezium ts_ms
    _deleted UInt8        -- 1 si delete, 0 sinon
)
ENGINE = ReplacingMergeTree(_version)
ORDER BY id;

-- -------------------------
-- ORDERS
-- -------------------------
CREATE TABLE IF NOT EXISTS orders (
    id UInt32,
    user_id UInt32,
    amount Decimal(10,2),

    _op String,
    _version UInt64,
    _deleted UInt8
)
ENGINE = ReplacingMergeTree(_version)
ORDER BY id;
-- ORDERS
-- CREATE TABLE IF NOT EXISTS orders (
--     o_id         UInt32,
--     o_d_id       UInt32,
--     o_w_id       UInt32,
--     o_c_id       UInt32,
--     o_entry_d    DateTime,
--     o_carrier_id UInt32,
--     o_ol_cnt     UInt32,
--
--     _op      String,
--     _version UInt64,
--     _deleted UInt8
-- )
-- ENGINE = ReplacingMergeTree(_version)
-- ORDER BY (o_w_id, o_d_id, o_id);
--
-- -- WAREHOUSE
-- CREATE TABLE IF NOT EXISTS warehouse (
--     w_id      UInt32,
--     w_name    String,
--     w_address String,
--     w_tax     Decimal(4,4),   -- NUMERIC(4,4)
--     w_ytd     Decimal(12,2),  -- NUMERIC(12,2)
--
--     _op      String,
--     _version UInt64,
--     _deleted UInt8
-- )
-- ENGINE = ReplacingMergeTree(_version)
-- ORDER BY (w_id);
--
-- -- DISTRICT
-- CREATE TABLE IF NOT EXISTS district (
--     d_id        UInt32,
--     d_w_id      UInt32,
--     d_name      String,
--     d_address   String,
--     d_tax       Decimal(4,4),   -- NUMERIC(4,4)
--     d_ytd       Decimal(12,2),  -- NUMERIC(12,2)
--     d_next_o_id UInt32,
--
--     _op      String,
--     _version UInt64,
--     _deleted UInt8
-- )
-- ENGINE = ReplacingMergeTree(_version)
-- ORDER BY (d_w_id, d_id);
--
-- -- CUSTOMER
-- CREATE TABLE IF NOT EXISTS customer (
--     c_id       UInt32,
--     c_d_id     UInt32,
--     c_w_id     UInt32,
--     c_first    String,
--     c_last     String,
--     c_credit   String,
--     c_discount Decimal(4,4),   -- NUMERIC(4,4)
--     c_balance  Decimal(12,2),  -- NUMERIC(12,2)
--
--     _op      String,
--     _version UInt64,
--     _deleted UInt8
-- )
-- ENGINE = ReplacingMergeTree(_version)
-- ORDER BY (c_w_id, c_d_id, c_id);
--
-- -- ORDER_LINE
-- CREATE TABLE IF NOT EXISTS order_line (
--     ol_o_id        UInt32,
--     ol_d_id        UInt32,
--     ol_w_id        UInt32,
--     ol_number      UInt32,
--     ol_i_id        UInt32,
--     ol_supply_w_id UInt32,
--     ol_quantity    UInt32,
--     ol_amount      Decimal(12,2),  -- NUMERIC(12,2)
--
--     _op      String,
--     _version UInt64,
--     _deleted UInt8
-- )
-- ENGINE = ReplacingMergeTree(_version)
-- ORDER BY (ol_w_id, ol_d_id, ol_o_id, ol_number);
