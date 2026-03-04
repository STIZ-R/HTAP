INSERT INTO oorder
(o_w_id, o_d_id, o_id, o_c_id, o_entry_d, o_carrier_id, o_ol_cnt, o_all_local)
VALUES ($W_ID$, $D_ID$, $O_ID$, $C_ID$, NOW(), NULL, $OL_CNT$, $ALL_LOCAL$);

INSERT INTO order_line (
    ol_w_id, ol_d_id, ol_o_id, ol_number,
    ol_i_id, ol_supply_w_id, ol_quantity, ol_amount, ol_dist_info
)
SELECT
    $W_ID$,
    $D_ID$,
    $O_ID$,
    $OL_NO$,
    s.s_i_id,
    s.s_w_id,
    $QTY$,
    $AMOUNT$,
    $DIST_INFO$
FROM stock s
WHERE s.s_w_id = $W_ID$
ORDER BY random()
    LIMIT 1;