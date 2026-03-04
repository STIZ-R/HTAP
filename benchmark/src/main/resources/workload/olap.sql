-- Top revenue per order
SELECT ol_w_id AS o_w_id, ol_d_id AS o_d_id, ol_o_id AS o_c_id,
       SUM(ol_amount) AS revenue, COUNT(*) AS lines
FROM ORDER_LINE
GROUP BY ol_w_id, ol_d_id, ol_o_id
ORDER BY revenue DESC
LIMIT 10;

-- Freshness probe query
SELECT max(o_entry_d) AS last_ts
FROM OORDER;
