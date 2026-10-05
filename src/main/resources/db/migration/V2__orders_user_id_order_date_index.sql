-- KIRODEMO-003: supporting index for the order-history access path.
--
-- Orders are looked up filtered by user_id and sorted by order_date descending
-- (most-recent-first paging). This composite index makes that a range scan,
-- supporting the p95 < 200 ms target (NFR-1). Additive and immutable; to remove
-- it, add a new migration rather than editing this one.
CREATE INDEX idx_orders_user_id_order_date
    ON orders (user_id, order_date DESC);
