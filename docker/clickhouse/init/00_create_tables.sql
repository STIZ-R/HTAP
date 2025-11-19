CREATE TABLE IF NOT EXISTS users (
    id UInt32,
    nom String,
    email String
) ENGINE = MergeTree()
ORDER BY id;

CREATE TABLE IF NOT EXISTS n (
    id UInt32,
    nom String
) ENGINE = MergeTree()
ORDER BY id;
