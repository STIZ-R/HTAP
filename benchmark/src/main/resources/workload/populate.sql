-- Warehouses
INSERT INTO warehouse (w_id, w_name, w_street_1, w_street_2, w_city, w_state, w_zip, w_tax, w_ytd)
SELECT gs, 'Warehouse '||gs, 'Street1', 'Street2', 'City', 'FR', '000000000',
       round(random()::numeric * 0.05 + 0.05, 4), 0
FROM generate_series(1,5) gs
    ON CONFLICT (w_id) DO NOTHING;

-- Items  ← MANQUAIT
INSERT INTO item (i_id, i_name, i_price, i_data, i_im_id)
SELECT gs, 'Item '||gs, round((random()*100+1)::numeric, 2), 'data_'||gs, (random()*10000)::int+1
FROM generate_series(1, 100000) gs
    ON CONFLICT (i_id) DO NOTHING;

-- Districts
INSERT INTO district (d_w_id, d_id, d_name, d_street_1, d_street_2, d_city, d_state, d_zip, d_tax, d_ytd, d_next_o_id)
SELECT w.w_id, gs, 'District '||gs, 'Street1', 'Street2', 'City', 'FR', '000000000',
       round(random()::numeric * 0.03 + 0.04, 4), 0, 1
FROM warehouse w CROSS JOIN generate_series(1,5) gs
    ON CONFLICT (d_w_id, d_id) DO NOTHING;

-- Customers
INSERT INTO customer (
    c_w_id, c_d_id, c_id, c_first, c_last, c_middle,
    c_street_1, c_street_2, c_city, c_state, c_zip,
    c_phone, c_since, c_credit, c_credit_lim, c_discount,
    c_balance, c_ytd_payment, c_payment_cnt, c_delivery_cnt, c_data
)
SELECT d_w_id, d_id, gs, 'First'||gs, 'Last'||gs, 'OE',
       'Street1', 'Street2', 'City', 'FR', '000000000',
       '0000000000000000', NOW(),
       'GC', 50000.00, round(random()::numeric * 0.1, 4),
       0, 0, 1, 0, 'data'
FROM district CROSS JOIN generate_series(1, 100000) gs
    ON CONFLICT (c_w_id, c_d_id, c_id) DO NOTHING;

-- Stock
INSERT INTO stock (
    s_w_id, s_i_id, s_quantity, s_ytd, s_order_cnt, s_remote_cnt, s_data,
    s_dist_01, s_dist_02, s_dist_03, s_dist_04, s_dist_05,
    s_dist_06, s_dist_07, s_dist_08, s_dist_09, s_dist_10
)
SELECT w, i,
       (random()*100+10)::int, 0, 0, 0, 'stockdata',
       'dist01','dist02','dist03','dist04','dist05',
       'dist06','dist07','dist08','dist09','dist10'
FROM generate_series(1,5) w,
     generate_series(1,100000) i
    ON CONFLICT (s_w_id, s_i_id) DO NOTHING;


-- Orders
INSERT INTO oorder (o_w_id, o_d_id, o_id, o_c_id, o_entry_d, o_carrier_id, o_ol_cnt, o_all_local)
SELECT d_w_id, d_id, gs,
       (random()*100000)::int+1,
    NOW() - (random()*365)::int * interval '1 day',
       (random()*10)::int,
       1 + (random()*9)::int,
       1
FROM district CROSS JOIN generate_series(1, 100000) gs
ON CONFLICT (o_w_id, o_d_id, o_id) DO NOTHING;

--order line
INSERT INTO order_line (
    ol_w_id, ol_d_id, ol_o_id, ol_number,
    ol_i_id, ol_supply_w_id, ol_quantity, ol_amount, ol_dist_info
)
SELECT o.o_w_id, o.o_d_id, o.o_id, gs,
       s.s_i_id,
       s.s_w_id,
       (random()*10)::int+1,
    round((random()*1000)::numeric, 2),
       'dist_01_0000000000000000'
FROM oorder o
         JOIN stock s
              ON s.s_w_id = o.o_w_id
         CROSS JOIN generate_series(1, 5) gs;

-- Marker CDC
INSERT INTO start (id, debug) VALUES (0, 0)
    ON CONFLICT (id) DO NOTHING;
