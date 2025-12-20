-- ============================
--  TABLE USERS
-- ============================
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE
);

-- ============================
--  TABLE ORDERS
-- ============================
CREATE TABLE IF NOT EXISTS orders (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount NUMERIC(10,2) NOT NULL
);

INSERT INTO users (name,email) VALUES ('AA', 'a@a');
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

