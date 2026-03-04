-- =========================
-- Requête 1 : Top revenue par commande
-- =========================
SELECT
    ol_w_id,
    ol_d_id,
    ol_o_id,
    SUM(ol_amount) AS revenue,
    COUNT(*)       AS nb_lines
FROM ORDER_LINE
GROUP BY ol_w_id, ol_d_id, ol_o_id
ORDER BY revenue DESC
    LIMIT 20;

-- =========================
-- Requête 2 : CA par entrepôt / district
-- =========================
SELECT
    o.o_w_id,
    o.o_d_id,
    COUNT(*)         AS nb_orders,
    SUM(ol.ol_amount) AS total_revenue
FROM OORDER o
         JOIN ORDER_LINE ol
              ON o.o_w_id = ol.ol_w_id
                  AND o.o_d_id = ol.ol_d_id
                  AND o.o_id   = ol.ol_o_id
GROUP BY o.o_w_id, o.o_d_id
ORDER BY total_revenue DESC;


-- =========================
-- Requête 3 : Top clients par chiffre d'affaires
-- =========================
SELECT
    c.c_w_id,
    c.c_d_id,
    c.c_id,
    c.c_first,
    c.c_last,
    SUM(ol.ol_amount) AS total_spent
FROM CUSTOMER c
         JOIN OORDER o
              ON c.c_w_id = o.o_w_id
                  AND c.c_d_id = o.o_d_id
                  AND c.c_id   = o.o_c_id
         JOIN ORDER_LINE ol
              ON o.o_w_id = ol.ol_w_id
                  AND o.o_d_id = ol.ol_d_id
                  AND o.o_id   = ol.ol_o_id
GROUP BY c.c_w_id, c.c_d_id, c.c_id, c.c_first, c.c_last
ORDER BY total_spent DESC
    LIMIT 50;


-- =========================
-- Requête 4 : Analyse temporelle (charges lourdes scan)
-- =========================
SELECT
    toDateTime(o_entry_d / 1000) AS order_time,
    COUNT(*) AS nb_orders
FROM OORDER
GROUP BY order_time
ORDER BY order_time;

-- =========================
-- Requête 5 : Freshness probe (utilisée par ton worker)
-- =========================
SELECT max(o_entry_d) AS last_ts
FROM OORDER;
