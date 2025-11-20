CREATE TABLE IF NOT EXISTS users (
    id UInt32,
    name String,
    email String
) ENGINE = ReplacingMergeTree(id)
ORDER BY id;

CREATE TABLE IF NOT EXISTS n (
    id UInt32,
    name String
) ENGINE = ReplacingMergeTree(id)
ORDER BY id;
