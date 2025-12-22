-- ============================
--  TABLE USERS
-- ============================
--CREATE TABLE IF NOT EXISTS users (
--    id SERIAL PRIMARY KEY,
--    name TEXT NOT NULL,
--    email TEXT NOT NULL UNIQUE
--);

-- ============================
--  TABLE ORDERS
-- ============================
--CREATE TABLE IF NOT EXISTS orders (
--    id SERIAL PRIMARY KEY,
--    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
--    amount NUMERIC(10,2) NOT NULL
--);

--INSERT INTO users (name,email) VALUES ('AA', 'a@a');
-- =========================
-- TPC-C LIKE SCHEMA (simplifié)
-- =========================

--CREATE TABLE IF NOT EXISTS warehouse (
--    w_id       INTEGER PRIMARY KEY,
--    w_name     TEXT NOT NULL,
--    w_address  TEXT,
--    w_tax      NUMERIC(4,4),
--    w_ytd      NUMERIC(12,2) DEFAULT 0
--);

--CREATE TABLE IF NOT EXISTS district (
--    d_id       INTEGER,
--    d_w_id     INTEGER REFERENCES warehouse(w_id),
--    d_name     TEXT NOT NULL,
--    d_address  TEXT,
--    d_tax      NUMERIC(4,4),
--    d_ytd      NUMERIC(12,2) DEFAULT 0,
--    d_next_o_id INTEGER DEFAULT 1,
--    PRIMARY KEY (d_w_id, d_id)
--);

--CREATE TABLE IF NOT EXISTS customer (
--    c_id        INTEGER,
--    c_d_id      INTEGER,
--    c_w_id      INTEGER,
  --  c_first     TEXT NOT NULL,
    --c_last      TEXT NOT NULL,
    --c_credit    TEXT,
    --c_discount  NUMERIC(4,4),
    --c_balance   NUMERIC(12,2) DEFAULT 0,
    --PRIMARY KEY (c_w_id, c_d_id, c_id),
    --FOREIGN KEY (c_w_id, c_d_id) REFERENCES district(d_w_id, d_id)
--);

--CREATE TABLE IF NOT EXISTS orders (
--    o_id        INTEGER,
 --   o_d_id      INTEGER,
  --  o_w_id      INTEGER,
  --  o_c_id      INTEGER,
   -- o_entry_d   TIMESTAMP NOT NULL DEFAULT NOW(),
    --o_carrier_id INTEGER,
    --o_ol_cnt    INTEGER,
    --PRIMARY KEY (o_w_id, o_d_id, o_id),
    --FOREIGN KEY (o_w_id, o_d_id, o_c_id) REFERENCES customer(c_w_id, c_d_id, c_id)
--);

--CREATE TABLE IF NOT EXISTS order_line (
  --  ol_o_id     INTEGER,
  --  ol_d_id     INTEGER,
  --  ol_w_id     INTEGER,
  --  ol_number   INTEGER,
  --  ol_i_id     INTEGER,
  --  ol_supply_w_id INTEGER,
  --  ol_quantity INTEGER,
  --  ol_amount   NUMERIC(12,2),
  --  PRIMARY KEY (ol_w_id, ol_d_id, ol_o_id, ol_number)
--);


-- ==========
-- Dimensions
-- ==========

CREATE TABLE warehouse (
    w_id       INTEGER PRIMARY KEY,
    w_name     TEXT NOT NULL,
    w_city     TEXT,
    w_country  TEXT,
    w_tax      NUMERIC(4,4),
    w_ytd      NUMERIC(12,2) DEFAULT 0
);

CREATE TABLE district (
    d_w_id     INTEGER REFERENCES warehouse(w_id),
    d_id       INTEGER,
    d_name     TEXT NOT NULL,
    d_city     TEXT,
    d_tax      NUMERIC(4,4),
    d_ytd      NUMERIC(12,2) DEFAULT 0,
    d_next_o_id INTEGER DEFAULT 1,
    PRIMARY KEY (d_w_id, d_id)
);

CREATE TABLE customer (
    c_w_id      INTEGER,
    c_d_id      INTEGER,
    c_id        INTEGER,
    c_first     TEXT NOT NULL,
    c_last      TEXT NOT NULL,
    c_city      TEXT,
    c_nation    TEXT,
    c_credit    TEXT,
    c_discount  NUMERIC(4,4),
    c_balance   NUMERIC(12,2) DEFAULT 0,
    PRIMARY KEY (c_w_id, c_d_id, c_id),
    FOREIGN KEY (c_w_id, c_d_id) REFERENCES district(d_w_id, d_id)
);

-- ==========
-- Faits OLTP
-- ==========

CREATE TABLE orders (
    o_w_id      INTEGER,
    o_d_id      INTEGER,
    o_id        INTEGER,
    o_c_id      INTEGER,
    o_entry_d   TIMESTAMP NOT NULL DEFAULT now(),
    o_carrier_id INTEGER,
    o_ol_cnt    INTEGER,
    PRIMARY KEY (o_w_id, o_d_id, o_id),
    FOREIGN KEY (o_w_id, o_d_id, o_c_id)
        REFERENCES customer(c_w_id, c_d_id, c_id)
);

CREATE TABLE order_line (
    ol_w_id     INTEGER,
    ol_d_id     INTEGER,
    ol_o_id     INTEGER,
    ol_number   INTEGER,
    ol_i_id     INTEGER,
    ol_supply_w_id INTEGER,
    ol_quantity INTEGER,
    ol_amount   NUMERIC(12,2),
    PRIMARY KEY (ol_w_id, ol_d_id, ol_o_id, ol_number),
    FOREIGN KEY (ol_w_id, ol_d_id, ol_o_id)
        REFERENCES orders(o_w_id, o_d_id, o_id)
);

-- Index typiques TPC-C-like pour le TPS
CREATE INDEX idx_orders_customer
    ON orders (o_w_id, o_d_id, o_c_id, o_id);

CREATE INDEX idx_order_line_item
    ON order_line (ol_i_id);

CREATE TABLE start (
    id          INTEGER,
    debug       INTEGER,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS events (
    id       SERIAL PRIMARY KEY,
    payload  TEXT,
    ts_write TIMESTAMP NOT NULL DEFAULT NOW()
);


INSERT INTO start (id, debug) VALUES (0, 0);

INSERT INTO warehouse (w_id, w_name) VALUES (1, 'W1')
ON CONFLICT (w_id) DO NOTHING;

INSERT INTO district (d_w_id, d_id, d_name)
VALUES (1, 1, 'D1')
ON CONFLICT (d_w_id, d_id) DO NOTHING;

INSERT INTO customer (c_w_id, c_d_id, c_id, c_first, c_last)
VALUES (1, 1, 1, 'John', 'Doe')
ON CONFLICT (c_w_id, c_d_id, c_id) DO NOTHING;

