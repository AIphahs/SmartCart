CREATE TABLE stores (
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(255) NOT NULL,
    address   VARCHAR(500),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE receipts (
    id            BIGSERIAL PRIMARY KEY,
    store_id      BIGINT REFERENCES stores (id) ON DELETE SET NULL,
    purchase_date DATE,
    total_amount  DECIMAL(10, 2),
    currency      VARCHAR(3)         DEFAULT 'EUR',
    image_path    VARCHAR(500),
    raw_text      TEXT,
    created_at    TIMESTAMP          DEFAULT NOW()
);

CREATE TABLE receipt_items (
    id           BIGSERIAL PRIMARY KEY,
    receipt_id   BIGINT       NOT NULL REFERENCES receipts (id) ON DELETE CASCADE,
    product_name VARCHAR(255) NOT NULL,
    quantity     DECIMAL(10, 3),
    unit_price   DECIMAL(10, 2),
    total_price  DECIMAL(10, 2) NOT NULL,
    category     VARCHAR(100)
);

CREATE INDEX idx_receipts_store_id      ON receipts (store_id);
CREATE INDEX idx_receipts_purchase_date ON receipts (purchase_date);
CREATE INDEX idx_receipt_items_receipt_id   ON receipt_items (receipt_id);
CREATE INDEX idx_receipt_items_category     ON receipt_items (category);
CREATE INDEX idx_receipt_items_product_name ON receipt_items (product_name);
