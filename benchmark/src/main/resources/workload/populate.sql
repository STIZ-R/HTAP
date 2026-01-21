-- =====================
-- Peuplement des tables
-- =====================

-- Warehouses (petit nombre)
INSERT INTO warehouse (w_id, w_name, w_city, w_country, w_tax, w_ytd)
SELECT gs AS w_id,
       'Warehouse ' || gs,
       'City ' || gs,
       'FR',
       round(random()::numeric * 0.05 + 0.05, 4),
       0
FROM generate_series(1,5) gs
    ON CONFLICT (w_id) DO NOTHING;

-- Districts
INSERT INTO district (d_w_id, d_id, d_name, d_city, d_tax, d_ytd, d_next_o_id)
SELECT w_id, gs,
       'District ' || gs,
       'City ' || gs,
       round(random()::numeric * 0.03 + 0.04, 4),
       0,
       1
FROM warehouse
         CROSS JOIN generate_series(1,5) gs
    ON CONFLICT (d_w_id,d_id) DO NOTHING;

-- Customers (100k par district)
INSERT INTO customer (c_w_id,c_d_id,c_id,c_first,c_last,c_city,c_nation,c_credit,c_discount,c_balance)
SELECT d_w_id,d_id, gs,
       'First'||gs,'Last'||gs,
       'City'||gs,'FR',
       'GC',
       round(random()::numeric * 0.1,4),
       0
FROM district
         CROSS JOIN generate_series(1,100000) gs
    ON CONFLICT (c_w_id,c_d_id,c_id) DO NOTHING;

-- Orders (100k par district)
INSERT INTO orders (o_w_id,o_d_id,o_id,o_c_id,o_entry_d,o_carrier_id,o_ol_cnt)
SELECT d_w_id,d_id, gs,
       (random()*100000)::int+1,
    now() - (random()*365)::int * interval '1 day',
       (random()*10)::int,
       (random()*10)::int
FROM district
    CROSS JOIN generate_series(1,100000) gs
ON CONFLICT (o_w_id,o_d_id,o_id) DO NOTHING;

-- Order lines (10 lignes par order)
INSERT INTO order_line (ol_w_id,ol_d_id,ol_o_id,ol_number,ol_i_id,ol_supply_w_id,ol_quantity,ol_amount)
SELECT o_w_id,o_d_id,o_id, gs,
       (random()*100000)::int +1,
    o_w_id,
       (random()*100)::int,
    round(random()*1000,2)
FROM orders
         CROSS JOIN generate_series(1,10) gs
    ON CONFLICT (ol_w_id,ol_d_id,ol_o_id,ol_number) DO NOTHING;

-- Table start (marker pour CDC)
INSERT INTO start (id, debug) VALUES (0,0)
    ON CONFLICT (id) DO NOTHING;

-- Table events (vide pour l'instant)
INSERT INTO events (payload) VALUES ('Init')
    ON CONFLICT (id) DO NOTHING;
