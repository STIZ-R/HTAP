-- -- =====================================================
-- -- Script ClickHouse TPC-C + TPC-H - DateTime64(3)
-- -- ReplacingMergeTree
-- -- Tous les timestamps utilisent DateTime64(3)
-- -- =====================================================
--
-- -- -----------------------------
-- -- Table REGION
-- -- -----------------------------
-- CREATE TABLE REGION (
--                         R_REGIONKEY Int32,
--                         R_NAME String,
--                         R_COMMENT String,
--                         _OP String,
--                         _VERSION UInt64,
--                         _DELETED UInt8
-- ) ENGINE = ReplacingMergeTree(_VERSION)
-- ORDER BY R_REGIONKEY;
--
-- -- -----------------------------
-- -- Table NATION
-- -- -----------------------------
-- CREATE TABLE NATION (
--                         N_NATIONKEY Int32,
--                         N_NAME String,
--                         N_REGIONKEY Int32,
--                         N_COMMENT String,
--                         _OP String,
--                         _VERSION UInt64,
--                         _DELETED UInt8
-- ) ENGINE = ReplacingMergeTree(_VERSION)
-- ORDER BY N_NATIONKEY;
--
-- -- -----------------------------
-- -- Table SUPPLIER
-- -- -----------------------------
-- CREATE TABLE SUPPLIER (
--                           SU_SUPPKEY Int32,
--                           SU_NAME String,
--                           SU_ADDRESS String,
--                           SU_NATIONKEY Int32,
--                           SU_PHONE String,
--                           SU_ACCTBAL Float64,
--                           SU_COMMENT String,
--                           _OP String,
--                           _VERSION UInt64,
--                           _DELETED UInt8
-- ) ENGINE = ReplacingMergeTree(_VERSION)
-- ORDER BY SU_SUPPKEY;
--
-- -- -----------------------------
-- -- Table WAREHOUSE
-- -- -----------------------------
-- CREATE TABLE WAREHOUSE (
--                            W_ID Int32,
--                            W_YTD Float64,
--                            W_TAX Float64,
--                            W_NAME String,
--                            W_STREET_1 String,
--                            W_STREET_2 String,
--                            W_CITY String,
--                            W_STATE String,
--                            W_ZIP String,
--                            _OP String,
--                            _VERSION UInt64,
--                            _DELETED UInt8
-- ) ENGINE = ReplacingMergeTree(_VERSION)
-- ORDER BY W_ID;
--
-- -- -----------------------------
-- -- Table ITEM
-- -- -----------------------------
-- CREATE TABLE ITEM (
--                       I_ID Int32,
--                       I_NAME String,
--                       I_PRICE Float64,
--                       I_DATA String,
--                       I_IM_ID Int32,
--                       _OP String,
--                       _VERSION UInt64,
--                       _DELETED UInt8
-- ) ENGINE = ReplacingMergeTree(_VERSION)
-- ORDER BY I_ID;
--
-- -- -----------------------------
-- -- Table CUSTOMER (PK composite)
-- -- -----------------------------
-- CREATE TABLE CUSTOMER (
--                           C_W_ID Int32,
--                           C_D_ID Int32,
--                           C_ID Int32,
--                           C_DISCOUNT Float64,
--                           C_CREDIT String,
--                           C_LAST String,
--                           C_FIRST String,
--                           C_CREDIT_LIM Float64,
--                           C_BALANCE Float64,
--                           C_YTD_PAYMENT Float64,
--                           C_PAYMENT_CNT Int32,
--                           C_DELIVERY_CNT Int32,
--                           C_STREET_1 String,
--                           C_STREET_2 String,
--                           C_CITY String,
--                           C_STATE String,
--                           C_ZIP String,
--                           C_PHONE String,
--                           C_SINCE DateTime64(3),
--                           C_MIDDLE String,
--                           C_DATA String,
--                           _OP String,
--                           _VERSION UInt64,
--                           _DELETED UInt8
-- ) ENGINE = ReplacingMergeTree(_VERSION)
-- ORDER BY (C_W_ID, C_D_ID, C_ID);
--
-- -- -----------------------------
-- -- Table DISTRICT (PK composite)
-- -- -----------------------------
-- CREATE TABLE DISTRICT (
--                           D_W_ID Int32,
--                           D_ID Int32,
--                           D_YTD Float64,
--                           D_TAX Float64,
--                           D_NEXT_O_ID Int32,
--                           D_NAME String,
--                           D_STREET_1 String,
--                           D_STREET_2 String,
--                           D_CITY String,
--                           D_STATE String,
--                           D_ZIP String,
--                           _OP String,
--                           _VERSION UInt64,
--                           _DELETED UInt8
-- ) ENGINE = ReplacingMergeTree(_VERSION)
-- ORDER BY (D_W_ID, D_ID);
--
-- -- -----------------------------
-- -- Table STOCK (PK composite)
-- -- -----------------------------
-- CREATE TABLE STOCK (
--                        S_W_ID Int32,
--                        S_I_ID Int32,
--                        S_QUANTITY Float64,
--                        S_YTD Float64,
--                        S_ORDER_CNT Int32,
--                        S_REMOTE_CNT Int32,
--                        S_DATA String,
--                        S_DIST_01 String,
--                        S_DIST_02 String,
--                        S_DIST_03 String,
--                        S_DIST_04 String,
--                        S_DIST_05 String,
--                        S_DIST_06 String,
--                        S_DIST_07 String,
--                        S_DIST_08 String,
--                        S_DIST_09 String,
--                        S_DIST_10 String,
--                        _OP String,
--                        _VERSION UInt64,
--                        _DELETED UInt8
-- ) ENGINE = ReplacingMergeTree(_VERSION)
-- ORDER BY (S_W_ID, S_I_ID);
--
-- -- -----------------------------
-- -- Table OORDER (PK composite)
-- -- -----------------------------
-- CREATE TABLE OORDER (
--                         O_W_ID Int32,
--                         O_D_ID Int32,
--                         O_ID Int32,
--                         O_C_ID Int32,
--                         O_CARRIER_ID Nullable(Int32),
--                         O_OL_CNT Float64,
--                         O_ALL_LOCAL Float64,
--                         O_ENTRY_D DateTime64(3),
--                         _OP String,
--                         _VERSION UInt64,
--                         _DELETED UInt8
-- ) ENGINE = ReplacingMergeTree(_VERSION)
-- ORDER BY (O_W_ID, O_D_ID, O_ID);
--
-- -- -----------------------------
-- -- Table NEW_ORDER (PK composite)
-- -- -----------------------------
-- CREATE TABLE NEW_ORDER (
--                            NO_W_ID Int32,
--                            NO_D_ID Int32,
--                            NO_O_ID Int32,
--                            _OP String,
--                            _VERSION UInt64,
--                            _DELETED UInt8
-- ) ENGINE = ReplacingMergeTree(_VERSION)
-- ORDER BY (NO_W_ID, NO_D_ID, NO_O_ID);
--
-- -- -----------------------------
-- -- Table ORDER_LINE (PK composite)
-- -- -----------------------------
-- CREATE TABLE ORDER_LINE (
--                             OL_W_ID Int32,
--                             OL_D_ID Int32,
--                             OL_O_ID Int32,
--                             OL_NUMBER Int32,
--                             OL_I_ID Int32,
--                             OL_DELIVERY_D DateTime64(3),
--                             OL_AMOUNT Float64,
--                             OL_SUPPLY_W_ID Int32,
--                             OL_QUANTITY Float64,
--                             OL_DIST_INFO String,
--                             _OP String,
--                             _VERSION UInt64,
--                             _DELETED UInt8
-- ) ENGINE = ReplacingMergeTree(_VERSION)
-- ORDER BY (OL_W_ID, OL_D_ID, OL_O_ID, OL_NUMBER);
--
-- -- -----------------------------
-- -- Table HISTORY (pas de PK naturel → ordre temporel)
-- -- -----------------------------
-- CREATE TABLE HISTORY (
--                          H_C_ID Int32,
--                          H_C_D_ID Int32,
--                          H_C_W_ID Int32,
--                          H_D_ID Int32,
--                          H_W_ID Int32,
--                          H_DATE DateTime64(3),
--                          H_AMOUNT Float64,
--                          H_DATA String,
--                          _OP String,
--                          _VERSION UInt64,
--                          _DELETED UInt8
-- ) ENGINE = ReplacingMergeTree(_VERSION)
-- ORDER BY (H_C_W_ID, H_C_D_ID, H_C_ID, H_DATE);


-- =====================================================
-- Script ClickHouse TPC-C + TPC-H - DateTime64(3)
-- ReplacingMergeTree
-- Tous les timestamps utilisent DateTime64(3)
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
) ENGINE = ReplacingMergeTree(_version)
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
) ENGINE = ReplacingMergeTree(_version)
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
) ENGINE = ReplacingMergeTree(_version)
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
) ENGINE = ReplacingMergeTree(_version)
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
) ENGINE = ReplacingMergeTree(_version)
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
                          c_since DateTime64(3),
                          c_middle String,
                          c_data String,

                          _op String,
                          _version UInt64,
                          _deleted UInt8
) ENGINE = ReplacingMergeTree(_version)
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
) ENGINE = ReplacingMergeTree(_version)
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
) ENGINE = ReplacingMergeTree(_version)
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
                        o_entry_d DateTime64(3),

                        _op String,
                        _version UInt64,
                        _deleted UInt8
) ENGINE = ReplacingMergeTree(_version)
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
) ENGINE = ReplacingMergeTree(_version)
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
                            ol_delivery_d DateTime64(3),
                            ol_amount Float64,
                            ol_supply_w_id Int32,
                            ol_quantity Float64,
                            ol_dist_info String,

                            _op String,
                            _version UInt64,
                            _deleted UInt8
) ENGINE = ReplacingMergeTree(_version)
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
                         h_date DateTime64(3),
                         h_amount Float64,
                         h_data String,

                         _op String,
                         _version UInt64,
                         _deleted UInt8
) ENGINE = ReplacingMergeTree(_version)
ORDER BY (h_c_w_id, h_c_d_id, h_c_id, h_date);

CREATE TABLE START (
                       id          UInt32,
                       debug       UInt32,

                       _op      String,
                       _version UInt64,
                       _deleted UInt8
)
    ENGINE = ReplacingMergeTree(_version)
ORDER BY (id);