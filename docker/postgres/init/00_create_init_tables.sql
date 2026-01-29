-- Script de création automatique des tables TPC-C
-- Basé sur les métadonnées fournies

-- Table: region (pas de FK)
CREATE TABLE region (
                        r_regionkey integer NOT NULL,
                        r_name character(55) NOT NULL,
                        r_comment character(152) NOT NULL,
                        CONSTRAINT region_pkey PRIMARY KEY (r_regionkey)
);

-- Table: nation (FK vers region)
CREATE TABLE nation (
                        n_nationkey integer NOT NULL,
                        n_name character(25) NOT NULL,
                        n_regionkey integer NOT NULL,
                        n_comment character(152) NOT NULL,
                        CONSTRAINT nation_pkey PRIMARY KEY (n_nationkey),
                        CONSTRAINT nation_region_fkey FOREIGN KEY (n_regionkey) REFERENCES region(r_regionkey)
);

-- Table: supplier (FK vers nation)
CREATE TABLE supplier (
                          su_suppkey integer NOT NULL,
                          su_name character(25) NOT NULL,
                          su_address character varying(40) NOT NULL,
                          su_nationkey integer NOT NULL,
                          su_phone character(15) NOT NULL,
                          su_acctbal numeric(12,2) NOT NULL,
                          su_comment character(101) NOT NULL,
                          CONSTRAINT supplier_pkey PRIMARY KEY (su_suppkey),
                          CONSTRAINT supplier_nation_fkey FOREIGN KEY (su_nationkey) REFERENCES nation(n_nationkey)
);

-- Table: warehouse (pas de FK)
CREATE TABLE warehouse (
                           w_id integer NOT NULL,
                           w_ytd numeric(12,2) NOT NULL,
                           w_tax numeric(5,4) NOT NULL,
                           w_name character varying(10) NOT NULL,
                           w_street_1 character varying(20) NOT NULL,
                           w_street_2 character varying(20) NOT NULL,
                           w_city character varying(20) NOT NULL,
                           w_state character(2) NOT NULL,
                           w_zip character(9) NOT NULL,
                           CONSTRAINT warehouse_pkey PRIMARY KEY (w_id)
);

-- Table: item (pas de FK)
CREATE TABLE item (
                      i_id integer NOT NULL,
                      i_name character varying(24) NOT NULL,
                      i_price numeric(5,2) NOT NULL,
                      i_data character varying(50) NOT NULL,
                      i_im_id integer NOT NULL,
                      CONSTRAINT item_pkey PRIMARY KEY (i_id)
);

-- Table: customer (PK composite)
CREATE TABLE customer (
                          c_w_id integer NOT NULL,
                          c_d_id integer NOT NULL,
                          c_id integer NOT NULL,
                          c_discount numeric(5,4) NOT NULL,
                          c_credit character(2) NOT NULL,
                          c_last character varying(16) NOT NULL,
                          c_first character varying(16) NOT NULL,
                          c_credit_lim numeric(12,2) NOT NULL,
                          c_balance numeric(12,2) NOT NULL,
                          c_ytd_payment double precision NOT NULL,
                          c_payment_cnt integer NOT NULL,
                          c_delivery_cnt integer NOT NULL,
                          c_street_1 character varying(20) NOT NULL,
                          c_street_2 character varying(20) NOT NULL,
                          c_city character varying(20) NOT NULL,
                          c_state character(2) NOT NULL,
                          c_zip character(9) NOT NULL,
                          c_phone character(16) NOT NULL,
                          c_since timestamp without time zone NOT NULL,
                          c_middle character(2) NOT NULL,
                          c_data character varying(500) NOT NULL,
                          CONSTRAINT customer_pkey PRIMARY KEY (c_w_id, c_d_id, c_id)
);

-- Table: district (PK composite, FK vers warehouse)
CREATE TABLE district (
                          d_w_id integer NOT NULL,
                          d_id integer NOT NULL,
                          d_ytd numeric(12,2) NOT NULL,
                          d_tax numeric(5,4) NOT NULL,
                          d_next_o_id integer NOT NULL,
                          d_name character varying(10) NOT NULL,
                          d_street_1 character varying(20) NOT NULL,
                          d_street_2 character varying(20) NOT NULL,
                          d_city character varying(20) NOT NULL,
                          d_state character(2) NOT NULL,
                          d_zip character(9) NOT NULL,
                          CONSTRAINT district_pkey PRIMARY KEY (d_w_id, d_id),
                          CONSTRAINT district_warehouse_fkey FOREIGN KEY (d_w_id) REFERENCES warehouse(w_id)
);

-- Table: stock (PK composite, FK vers item, warehouse)
CREATE TABLE stock (
                       s_w_id integer NOT NULL,
                       s_i_id integer NOT NULL,
                       s_quantity numeric(4,0) NOT NULL,
                       s_ytd numeric(8,2) NOT NULL,
                       s_order_cnt integer NOT NULL,
                       s_remote_cnt integer NOT NULL,
                       s_data character varying(50) NOT NULL,
                       s_dist_01 character(24) NOT NULL,
                       s_dist_02 character(24) NOT NULL,
                       s_dist_03 character(24) NOT NULL,
                       s_dist_04 character(24) NOT NULL,
                       s_dist_05 character(24) NOT NULL,
                       s_dist_06 character(24) NOT NULL,
                       s_dist_07 character(24) NOT NULL,
                       s_dist_08 character(24) NOT NULL,
                       s_dist_09 character(24) NOT NULL,
                       s_dist_10 character(24) NOT NULL,
                       CONSTRAINT stock_pkey PRIMARY KEY (s_w_id, s_i_id),
                       CONSTRAINT stock_item_fkey FOREIGN KEY (s_i_id) REFERENCES item(i_id),
                       CONSTRAINT stock_warehouse_fkey FOREIGN KEY (s_w_id) REFERENCES warehouse(w_id)
);

-- Table: oorder (PK composite + UNIQUE)
CREATE TABLE oorder (
                        o_w_id integer NOT NULL,
                        o_d_id integer NOT NULL,
                        o_id integer NOT NULL,
                        o_c_id integer NOT NULL,
                        o_carrier_id integer,
                        o_ol_cnt numeric(2,0) NOT NULL,
                        o_all_local numeric(1,0) NOT NULL,
                        o_entry_d timestamp without time zone NOT NULL,
                        CONSTRAINT oorder_pkey PRIMARY KEY (o_w_id, o_d_id, o_id),
                        CONSTRAINT oorder_unique UNIQUE (o_w_id, o_d_id, o_id, o_c_id),
                        CONSTRAINT oorder_customer_fkey FOREIGN KEY (o_w_id, o_d_id, o_c_id) REFERENCES customer(c_w_id, c_d_id, c_id),
                        CONSTRAINT oorder_district_fkey FOREIGN KEY (o_w_id, o_d_id) REFERENCES district(d_w_id, d_id)
);

-- Table: new_order (PK composite)
CREATE TABLE new_order (
                           no_w_id integer NOT NULL,
                           no_d_id integer NOT NULL,
                           no_o_id integer NOT NULL,
                           CONSTRAINT new_order_pkey PRIMARY KEY (no_w_id, no_d_id, no_o_id),
                           CONSTRAINT new_order_district_fkey FOREIGN KEY (no_w_id, no_d_id) REFERENCES district(d_w_id, d_id)
);

-- Table: order_line (PK composite, FK multiples)
CREATE TABLE order_line (
                            ol_w_id integer NOT NULL,
                            ol_d_id integer NOT NULL,
                            ol_o_id integer NOT NULL,
                            ol_number integer NOT NULL,
                            ol_i_id integer NOT NULL,
                            ol_delivery_d timestamp without time zone,
                            ol_amount numeric(6,2) NOT NULL,
                            ol_supply_w_id integer NOT NULL,
                            ol_quantity numeric(2,0) NOT NULL,
                            ol_dist_info character(24) NOT NULL,
                            CONSTRAINT order_line_pkey PRIMARY KEY (ol_w_id, ol_d_id, ol_o_id, ol_number),
                            CONSTRAINT order_line_item_fkey FOREIGN KEY (ol_i_id) REFERENCES item(i_id),
                            CONSTRAINT order_line_oorder_fkey FOREIGN KEY (ol_w_id, ol_d_id, ol_o_id) REFERENCES oorder(o_w_id, o_d_id, o_id),
                            CONSTRAINT order_line_stock_fkey FOREIGN KEY (ol_supply_w_id, ol_i_id) REFERENCES stock(s_w_id, s_i_id)
);

-- Table: history (pas de PK explicite, FK vers customer + district)
CREATE TABLE history (
                         h_c_id integer NOT NULL,
                         h_c_d_id integer NOT NULL,
                         h_c_w_id integer NOT NULL,
                         h_d_id integer NOT NULL,
                         h_w_id integer NOT NULL,
                         h_date timestamp without time zone NOT NULL,
                         h_amount numeric(6,2) NOT NULL,
                         h_data character varying(24) NOT NULL,
                         CONSTRAINT history_customer_fkey FOREIGN KEY (h_c_w_id, h_c_d_id, h_c_id) REFERENCES customer(c_w_id, c_d_id, c_id),
                         CONSTRAINT history_district_fkey FOREIGN KEY (h_w_id, h_d_id) REFERENCES district(d_w_id, d_id)
);
