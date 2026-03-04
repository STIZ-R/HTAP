-- ========================
-- DROP si relance
-- ========================
DROP TABLE IF EXISTS order_line CASCADE;
DROP TABLE IF EXISTS new_order CASCADE;
DROP TABLE IF EXISTS oorder CASCADE;
DROP TABLE IF EXISTS history CASCADE;
DROP TABLE IF EXISTS stock CASCADE;
DROP TABLE IF EXISTS customer CASCADE;
DROP TABLE IF EXISTS district CASCADE;
DROP TABLE IF EXISTS item CASCADE;
DROP TABLE IF EXISTS warehouse CASCADE;
DROP TABLE IF EXISTS supplier CASCADE;
DROP TABLE IF EXISTS nation CASCADE;
DROP TABLE IF EXISTS region CASCADE;

-- ========================
-- TPC-H
-- ========================

CREATE TABLE region (
                        r_regionkey integer PRIMARY KEY,
                        r_name char(55) NOT NULL,
                        r_comment char(152) NOT NULL
);

CREATE TABLE nation (
                        n_nationkey integer PRIMARY KEY,
                        n_name char(25) NOT NULL,
                        n_regionkey integer NOT NULL REFERENCES region(r_regionkey),
                        n_comment char(152) NOT NULL
);

CREATE TABLE supplier (
                          su_suppkey integer PRIMARY KEY,
                          su_name char(25) NOT NULL,
                          su_address varchar(40) NOT NULL,
                          su_nationkey integer NOT NULL REFERENCES nation(n_nationkey),
                          su_phone char(15) NOT NULL,
                          su_acctbal numeric(12,2) NOT NULL,
                          su_comment char(101) NOT NULL
);

-- ========================
-- TPC-C
-- ========================

CREATE TABLE warehouse (
                           w_id integer PRIMARY KEY,
                           w_ytd numeric(12,2) NOT NULL,
                           w_tax numeric(5,4) NOT NULL,
                           w_name varchar(10) NOT NULL,
                           w_street_1 varchar(20) NOT NULL,
                           w_street_2 varchar(20) NOT NULL,
                           w_city varchar(20) NOT NULL,
                           w_state char(2) NOT NULL,
                           w_zip char(9) NOT NULL
);

CREATE TABLE item (
                      i_id integer PRIMARY KEY,
                      i_name varchar(24) NOT NULL,
                      i_price numeric(5,2) NOT NULL,
                      i_data varchar(50) NOT NULL,
                      i_im_id integer NOT NULL
);

CREATE TABLE district (
                          d_w_id integer NOT NULL,
                          d_id integer NOT NULL,
                          d_ytd numeric(12,2) NOT NULL,
                          d_tax numeric(5,4) NOT NULL,
                          d_next_o_id integer NOT NULL,
                          d_name varchar(10) NOT NULL,
                          d_street_1 varchar(20) NOT NULL,
                          d_street_2 varchar(20) NOT NULL,
                          d_city varchar(20) NOT NULL,
                          d_state char(2) NOT NULL,
                          d_zip char(9) NOT NULL,
                          PRIMARY KEY (d_w_id, d_id),
                          FOREIGN KEY (d_w_id) REFERENCES warehouse(w_id)
);

CREATE TABLE customer (
                          c_w_id integer NOT NULL,
                          c_d_id integer NOT NULL,
                          c_id integer NOT NULL,
                          c_discount numeric(5,4) NOT NULL,
                          c_credit char(2) NOT NULL,
                          c_last varchar(16) NOT NULL,
                          c_first varchar(16) NOT NULL,
                          c_credit_lim numeric(12,2) NOT NULL,
                          c_balance numeric(12,2) NOT NULL,
                          c_ytd_payment double precision NOT NULL,
                          c_payment_cnt integer NOT NULL,
                          c_delivery_cnt integer NOT NULL,
                          c_street_1 varchar(20) NOT NULL,
                          c_street_2 varchar(20) NOT NULL,
                          c_city varchar(20) NOT NULL,
                          c_state char(2) NOT NULL,
                          c_zip char(9) NOT NULL,
                          c_phone char(16) NOT NULL,
                          c_since timestamptz NOT NULL,
                          c_middle char(2) NOT NULL,
                          c_data varchar(500) NOT NULL,
                          PRIMARY KEY (c_w_id, c_d_id, c_id)
);

CREATE TABLE stock (
                       s_w_id integer NOT NULL,
                       s_i_id integer NOT NULL,
                       s_quantity numeric(4,0) NOT NULL,
                       s_ytd numeric(8,2) NOT NULL,
                       s_order_cnt integer NOT NULL,
                       s_remote_cnt integer NOT NULL,
                       s_data varchar(50) NOT NULL,
                       s_dist_01 char(24) NOT NULL,
                       s_dist_02 char(24) NOT NULL,
                       s_dist_03 char(24) NOT NULL,
                       s_dist_04 char(24) NOT NULL,
                       s_dist_05 char(24) NOT NULL,
                       s_dist_06 char(24) NOT NULL,
                       s_dist_07 char(24) NOT NULL,
                       s_dist_08 char(24) NOT NULL,
                       s_dist_09 char(24) NOT NULL,
                       s_dist_10 char(24) NOT NULL,
                       PRIMARY KEY (s_w_id, s_i_id),
                       FOREIGN KEY (s_w_id) REFERENCES warehouse(w_id),
                       FOREIGN KEY (s_i_id) REFERENCES item(i_id)
);

CREATE TABLE oorder (
                        o_w_id integer NOT NULL,
                        o_d_id integer NOT NULL,
                        o_id integer NOT NULL,
                        o_c_id integer NOT NULL,
                        o_carrier_id integer,
                        o_ol_cnt numeric(2,0) NOT NULL,
                        o_all_local numeric(1,0) NOT NULL,
                        o_entry_d timestamptz NOT NULL,
                        PRIMARY KEY (o_w_id, o_d_id, o_id),
                        FOREIGN KEY (o_w_id, o_d_id) REFERENCES district(d_w_id, d_id)
);

CREATE TABLE new_order (
                           no_w_id integer NOT NULL,
                           no_d_id integer NOT NULL,
                           no_o_id integer NOT NULL,
                           PRIMARY KEY (no_w_id, no_d_id, no_o_id),
                           FOREIGN KEY (no_w_id, no_d_id) REFERENCES district(d_w_id, d_id)
);

CREATE TABLE order_line (
                            ol_w_id integer NOT NULL,
                            ol_d_id integer NOT NULL,
                            ol_o_id integer NOT NULL,
                            ol_number integer NOT NULL,
                            ol_i_id integer NOT NULL,
                            ol_delivery_d timestamptz,
                            ol_amount numeric(6,2) NOT NULL,
                            ol_supply_w_id integer NOT NULL,
                            ol_quantity numeric(2,0) NOT NULL,
                            ol_dist_info char(24) NOT NULL,
                            PRIMARY KEY (ol_w_id, ol_d_id, ol_o_id, ol_number),
                            FOREIGN KEY (ol_w_id, ol_d_id, ol_o_id) REFERENCES oorder(o_w_id, o_d_id, o_id),
                            FOREIGN KEY (ol_supply_w_id, ol_i_id) REFERENCES stock(s_w_id, s_i_id),
                            FOREIGN KEY (ol_i_id) REFERENCES item(i_id)
);

CREATE TABLE history (
                         h_c_id integer NOT NULL,
                         h_c_d_id integer NOT NULL,
                         h_c_w_id integer NOT NULL,
                         h_d_id integer NOT NULL,
                         h_w_id integer NOT NULL,
                         h_date timestamptz NOT NULL,
                         h_amount numeric(6,2) NOT NULL,
                         h_data varchar(24) NOT NULL,
                         FOREIGN KEY (h_w_id, h_d_id) REFERENCES district(d_w_id, d_id)
);

-- ========================
-- Réplication logique (Debezium)
-- ========================
ALTER TABLE history REPLICA IDENTITY FULL;

CREATE TABLE start (
                       id          INTEGER,
                       debug       INTEGER,
                       PRIMARY KEY (id)
);

-- Warehouse minimal
INSERT INTO warehouse (w_id, w_name, w_street_1, w_street_2, w_city, w_state, w_zip, w_tax, w_ytd)
VALUES (1, 'W1', 'Street1', 'Street2', 'City1', 'FR', '000000000', 0.05, 0.00)
    ON CONFLICT (w_id) DO NOTHING;

-- District minimal (FK → warehouse)
INSERT INTO district (d_w_id, d_id, d_name, d_street_1, d_street_2, d_city, d_state, d_zip, d_tax, d_ytd, d_next_o_id)
VALUES (1, 1, 'D1', 'Street1', 'Street2', 'City1', 'FR', '000000000', 0.04, 0.00, 1)
    ON CONFLICT (d_w_id, d_id) DO NOTHING;

-- Customer minimal (FK → district)
INSERT INTO customer (
    c_w_id, c_d_id, c_id,
    c_first, c_last, c_middle,
    c_street_1, c_street_2, c_city, c_state, c_zip,
    c_phone, c_since,
    c_credit, c_credit_lim, c_discount,
    c_balance, c_ytd_payment, c_payment_cnt, c_delivery_cnt,
    c_data
)
VALUES (
           1, 1, 1,
           'John', 'Doe', 'OE',
           'Street1', 'Street2', 'City1', 'FR', '000000000',
           '0000000000000000', NOW(),
           'GC', 50000.00, 0.05,
           0.00, 0.00, 0, 0,
           'data'
       )
    ON CONFLICT (c_w_id, c_d_id, c_id) DO NOTHING;
