-- NewOrder
INSERT INTO orders
  (o_w_id, o_d_id, o_id, o_c_id, o_entry_d, o_carrier_id, o_ol_cnt)
VALUES ($W_ID$, $D_ID$, $O_ID$, $C_ID$, NOW(), NULL, $OL_CNT$);

INSERT INTO order_line
  (ol_w_id, ol_d_id, ol_o_id, ol_number, ol_i_id, ol_supply_w_id, ol_quantity, ol_amount)
VALUES ($W_ID$, $D_ID$, $O_ID$, $OL_NO$, $ITEM_ID$, $W_ID$, $QTY$, $AMOUNT$);
