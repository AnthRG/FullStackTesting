CREATE TABLE products (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(150)   NOT NULL,
    sku           VARCHAR(50)    NOT NULL UNIQUE,
    description   TEXT,
    category      VARCHAR(100)   NOT NULL,
    price         NUMERIC(12, 2) NOT NULL,
    quantity      INTEGER        NOT NULL DEFAULT 0,
    minimum_stock INTEGER        NOT NULL DEFAULT 0,
    status        VARCHAR(10)    NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_price         CHECK (price >= 0),
    CONSTRAINT chk_quantity      CHECK (quantity >= 0),
    CONSTRAINT chk_min_stock     CHECK (minimum_stock >= 0),
    CONSTRAINT chk_status        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
