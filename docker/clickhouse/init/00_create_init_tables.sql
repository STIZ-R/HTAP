-- -- Dimensions OLAP
-- CREATE TABLE warehouse (
--                            w_id      UInt32,
--                            w_name    String,
--                            w_city    String,
--                            w_country String,
--                            w_tax     Decimal(4,4),
--                            w_ytd     Decimal(12,2),
--                            _op      String,
--                            _version UInt64,
--                            _deleted UInt8
-- ) ENGINE = ReplacingMergeTree(_version)
-- ORDER BY w_id;
--
-- CREATE TABLE district (
--                           d_w_id      UInt32,
--                           d_id        UInt32,
--                           d_name      String,
--                           d_city      String,
--                           d_tax       Decimal(4,4),
--                           d_ytd       Decimal(12,2),
--                           d_next_o_id UInt32,
--                           _op      String,
--                           _version UInt64,
--                           _deleted UInt8
-- ) ENGINE = ReplacingMergeTree(_version)
-- ORDER BY (d_w_id, d_id);
--
-- CREATE TABLE customer (
--                           c_w_id      UInt32,
--                           c_d_id      UInt32,
--                           c_id        UInt32,
--                           c_first     String,
--                           c_last      String,
--                           c_city      String,
--                           c_nation    String,
--                           c_credit    String,
--                           c_discount  Decimal(4,4),
--                           c_balance   Decimal(12,2),
--                           _op      String,
--                           _version UInt64,
--                           _deleted UInt8
-- ) ENGINE = ReplacingMergeTree(_version)
-- ORDER BY (c_w_id, c_d_id, c_id);
--
-- -- Fact tables OLAP
-- CREATE TABLE orders (
--                         o_w_id      UInt32,
--                         o_d_id      UInt32,
--                         o_id        UInt32,
--                         o_c_id      UInt32,
--                         o_entry_d   UInt64,
--                         o_carrier_id UInt32,
--                         o_ol_cnt    UInt32,
--                         _op      String,
--                         _version UInt64,
--                         _deleted UInt8
-- ) ENGINE = ReplacingMergeTree(_version)
-- ORDER BY (o_w_id, o_d_id, o_id);
--
-- CREATE TABLE order_line (
--                             ol_w_id        UInt32,
--                             ol_d_id        UInt32,
--                             ol_o_id        UInt32,
--                             ol_number      UInt32,
--                             ol_i_id        UInt32,
--                             ol_supply_w_id UInt32,
--                             ol_quantity    UInt32,
--                             ol_amount      Decimal(12,2),
--                             _op      String,
--                             _version UInt64,
--                             _deleted UInt8
-- ) ENGINE = ReplacingMergeTree(_version)
-- ORDER BY (ol_w_id, ol_d_id, ol_o_id, ol_number);
--
-- CREATE TABLE events (
--                         id       UInt32,
--                         payload  String,
--                         ts_write DateTime,
--                         _op      String,
--                         _version UInt64,
--                         _deleted UInt8
-- ) ENGINE = ReplacingMergeTree(_version)
-- ORDER BY id;
-- =====================================================
-- Script ClickHouse TPC-C complet - ReplacingMergeTree
-- ORDER BY = PK original pour performance optimale
-- =====================================================

-- -----------------------------
-- Table region
-- -----------------------------
CREATE TABLE REGION (
                        r_regionkey Int32,
                        r_name String,
                        r_comment String,
                        _op String,
                        _version UInt64,
                        _deleted UInt8
)
    ENGINE = ReplacingMergeTree(_version)
ORDER BY r_regionkey;

-- -----------------------------
-- Table nation
-- -----------------------------
CREATE TABLE NATION (
                        n_nationkey Int32,
                        n_name String,
                        n_regionkey Int32,
                        n_comment String,
                        _op String,
                        _version UInt64,
                        _deleted UInt8
)
    ENGINE = ReplacingMergeTree(_version)
ORDER BY n_nationkey;

-- -----------------------------
-- Table supplier
-- -----------------------------
CREATE TABLE SUPPLIER (
                          su_suppkey Int32,
                          su_name String,
                          su_address String,
                          su_nationkey Int32,
                          su_phone String,
                          su_acctbal Float64,
                          su_comment String,
                          _op String,
                          _version UInt64,
                          _deleted UInt8
)
    ENGINE = ReplacingMergeTree(_version)
ORDER BY su_suppkey;

-- -----------------------------
-- Table warehouse
-- -----------------------------
CREATE TABLE WAREHOUSE (
                           w_id Int32,
                           w_ytd Float64,
                           w_tax Float64,
                           w_name String,
                           w_street_1 String,
                           w_street_2 String,
                           w_city String,
                           w_state String,
                           w_zip String,
                           _op String,
                           _version UInt64,
                           _deleted UInt8
)
    ENGINE = ReplacingMergeTree(_version)
ORDER BY w_id;

-- -----------------------------
-- Table item
-- -----------------------------
CREATE TABLE ITEM (
                      i_id Int32,
                      i_name String,
                      i_price Float64,
                      i_data String,
                      i_im_id Int32,
                      _op String,
                      _version UInt64,
                      _deleted UInt8
)
    ENGINE = ReplacingMergeTree(_version)
ORDER BY i_id;

-- -----------------------------
-- Table customer (PK composite)
-- -----------------------------
CREATE TABLE CUSTOMER (
                          c_w_id Int32,
                          c_d_id Int32,
                          c_id Int32,
                          c_discount Float64,
                          c_credit String,
                          c_last String,
                          c_first String,
                          c_credit_lim Float64,
                          c_balance Float64,
                          c_ytd_payment Float64,
                          c_payment_cnt Int32,
                          c_delivery_cnt Int32,
                          c_street_1 String,
                          c_street_2 String,
                          c_city String,
                          c_state String,
                          c_zip String,
                          c_phone String,
                          c_since DateTime,
                          c_middle String,
                          c_data String,
                          _op String,
                          _version UInt64,
                          _deleted UInt8
)
    ENGINE = ReplacingMergeTree(_version)
ORDER BY (c_w_id, c_d_id, c_id);

-- -----------------------------
-- Table district (PK composite)
-- -----------------------------
CREATE TABLE DISTRICT (
                          d_w_id Int32,
                          d_id Int32,
                          d_ytd Float64,
                          d_tax Float64,
                          d_next_o_id Int32,
                          d_name String,
                          d_street_1 String,
                          d_street_2 String,
                          d_city String,
                          d_state String,
                          d_zip String,
                          _op String,
                          _version UInt64,
                          _deleted UInt8
)
    ENGINE = ReplacingMergeTree(_version)
ORDER BY (d_w_id, d_id);

-- -----------------------------
-- Table stock (PK composite)
-- -----------------------------
CREATE TABLE STOCK (
                       s_w_id Int32,
                       s_i_id Int32,
                       s_quantity Float64,
                       s_ytd Float64,
                       s_order_cnt Int32,
                       s_remote_cnt Int32,
                       s_data String,
                       s_dist_01 String,
                       s_dist_02 String,
                       s_dist_03 String,
                       s_dist_04 String,
                       s_dist_05 String,
                       s_dist_06 String,
                       s_dist_07 String,
                       s_dist_08 String,
                       s_dist_09 String,
                       s_dist_10 String,
                       _op String,
                       _version UInt64,
                       _deleted UInt8
)
    ENGINE = ReplacingMergeTree(_version)
ORDER BY (s_w_id, s_i_id);

-- -----------------------------
-- Table oorder (PK composite)
-- -----------------------------
CREATE TABLE OORDER (
                        o_w_id Int32,
                        o_d_id Int32,
                        o_id Int32,
                        o_c_id Int32,
                        o_carrier_id Nullable(Int32),
                        o_ol_cnt Float64,
                        o_all_local Float64,
                        o_entry_d DateTime,
                        _op String,
                        _version UInt64,
                        _deleted UInt8
)
    ENGINE = ReplacingMergeTree(_version)
ORDER BY (o_w_id, o_d_id, o_id);

-- -----------------------------
-- Table new_order (PK composite)
-- -----------------------------
CREATE TABLE NEW_ORDER (
                           no_w_id Int32,
                           no_d_id Int32,
                           no_o_id Int32,
                           _op String,
                           _version UInt64,
                           _deleted UInt8
)
    ENGINE = ReplacingMergeTree(_version)
ORDER BY (no_w_id, no_d_id, no_o_id);

-- -----------------------------
-- Table order_line (PK composite)
-- -----------------------------
CREATE TABLE ORDER_LINE (
                            ol_w_id Int32,
                            ol_d_id Int32,
                            ol_o_id Int32,
                            ol_number Int32,
                            ol_i_id Int32,
                            ol_delivery_d Nullable(DateTime),
                            ol_amount Float64,
                            ol_supply_w_id Int32,
                            ol_quantity Float64,
                            ol_dist_info String,
                            _op String,
                            _version UInt64,
                            _deleted UInt8
)
    ENGINE = ReplacingMergeTree(_version)
ORDER BY (ol_w_id, ol_d_id, ol_o_id, ol_number);

-- -----------------------------
-- Table history (pas de PK naturel → ordre temporel)
-- -----------------------------
CREATE TABLE HISTORY (
                         h_c_id Int32,
                         h_c_d_id Int32,
                         h_c_w_id Int32,
                         h_d_id Int32,
                         h_w_id Int32,
                         h_date DateTime,
                         h_amount Float64,
                         h_data String,
                         _op String,
                         _version UInt64,
                         _deleted UInt8
)
    ENGINE = ReplacingMergeTree(_version)
ORDER BY (h_c_w_id, h_c_d_id, h_c_id, h_date);
