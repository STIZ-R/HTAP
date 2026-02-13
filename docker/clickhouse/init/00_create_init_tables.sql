



  --Dimensions OLAP
 CREATE TABLE warehouse (
                            w_id      UInt32,
                            w_name    String,
                            w_city    String,
                            w_country String,
                            w_tax     Decimal(4,4),
                            w_ytd     Decimal(12,2),
                            _op      String,
                            _version UInt64,
                            _deleted UInt8
 ) ENGINE = ReplacingMergeTree(_version)
 ORDER BY w_id;

 CREATE TABLE district (
                           d_w_id      UInt32,
                           d_id        UInt32,
                           d_name      String,
                           d_city      String,
                           d_tax       Decimal(4,4),
                           d_ytd       Decimal(12,2),
                           d_next_o_id UInt32,
                           _op      String,
                           _version UInt64,
                           _deleted UInt8
 ) ENGINE = ReplacingMergeTree(_version)
 ORDER BY (d_w_id, d_id);

 CREATE TABLE customer (
                           c_w_id      UInt32,
                           c_d_id      UInt32,
                           c_id        UInt32,
                           c_first     String,
                           c_last      String,
                           c_city      String,
                           c_nation    String,
                           c_credit    String,
                           c_discount  Decimal(4,4),
                           c_balance   Decimal(12,2),
                           _op      String,
                           _version UInt64,
                           _deleted UInt8
 ) ENGINE = ReplacingMergeTree(_version)
 ORDER BY (c_w_id, c_d_id, c_id);

  Fact tables OLAP
 CREATE TABLE orders (
                         o_w_id      UInt32,
                         o_d_id      UInt32,
                         o_id        UInt32,
                         o_c_id      UInt32,
                         o_entry_d   UInt64,
                         o_carrier_id UInt32,
                         o_ol_cnt    UInt32,
                         _op      String,
                         _version UInt64,
                         _deleted UInt8
 ) ENGINE = ReplacingMergeTree(_version)
 ORDER BY (o_w_id, o_d_id, o_id);

 CREATE TABLE order_line (
                             ol_w_id        UInt32,
                             ol_d_id        UInt32,
                             ol_o_id        UInt32,
                             ol_number      UInt32,
-                             ol_i_id        UInt32,
                             ol_supply_w_id UInt32,
                             ol_quantity    UInt32,
                             ol_amount      Decimal(12,2),
                             _op      String,
                             _version UInt64,
                             _deleted UInt8
 ) ENGINE = ReplacingMergeTree(_version)
 ORDER BY (ol_w_id, ol_d_id, ol_o_id, ol_number);

 CREATE TABLE events (
                         id       UInt32,
                         payload  String,
                         ts_write DateTime,
                         _op      String,
                         _version UInt64,
                         _deleted UInt8
 ) ENGINE = ReplacingMergeTree(_version)
ORDER BY id;