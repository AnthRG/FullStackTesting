CREATE TABLE stock_movements (
    id                BIGSERIAL PRIMARY KEY,
    product_id        BIGINT       NOT NULL,
    movement_type     VARCHAR(30)  NOT NULL,
    quantity          INTEGER      NOT NULL,
    previous_quantity INTEGER      NOT NULL,
    new_quantity      INTEGER      NOT NULL,
    user_id           VARCHAR(255) NOT NULL,
    observations      TEXT,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_stock_movements_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_movement_type     CHECK (movement_type IN ('IN', 'OUT', 'ADJUSTMENT')),
    CONSTRAINT chk_movement_quantity CHECK (quantity > 0),
    CONSTRAINT chk_new_quantity      CHECK (new_quantity >= 0)
);

CREATE INDEX idx_stock_movements_product ON stock_movements (product_id);
CREATE INDEX idx_stock_movements_created_at ON stock_movements (created_at);

CREATE TABLE stock_movements_aud (
    id                BIGINT  NOT NULL,
    rev               INTEGER NOT NULL,
    revtype           SMALLINT,
    product_id        BIGINT,
    movement_type     VARCHAR(30),
    quantity          INTEGER,
    previous_quantity INTEGER,
    new_quantity      INTEGER,
    user_id           VARCHAR(255),
    observations      TEXT,
    created_at        TIMESTAMP,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_stock_movements_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);
