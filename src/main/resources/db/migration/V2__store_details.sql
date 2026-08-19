ALTER TABLE stores ADD COLUMN phone VARCHAR(50);
ALTER TABLE stores ADD COLUMN website VARCHAR(255);

CREATE TABLE store_hours (
    id           BIGSERIAL PRIMARY KEY,
    store_id     BIGINT   NOT NULL REFERENCES stores (id) ON DELETE CASCADE,
    day_of_week  SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    open_time    TIME,
    close_time   TIME,
    closed       BOOLEAN  NOT NULL DEFAULT FALSE,
    UNIQUE (store_id, day_of_week)
);

CREATE INDEX idx_store_hours_store_id ON store_hours (store_id);
