---CREATE TABLE IF NOT EXISTS users (
---   id UInt32,
--    name String,
--    email String
--) ENGINE = ReplacingMergeTree(id)
--ORDER BY id;

--CREATE TABLE IF NOT EXISTS n (
--    id UInt32,
--    name String
--) ENGINE = ReplacingMergeTree(id)
--ORDER BY id;
---


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
    amount Float64,

    _op String,
    _version UInt64,
    _deleted UInt8
)
ENGINE = ReplacingMergeTree(_version)
ORDER BY id;
