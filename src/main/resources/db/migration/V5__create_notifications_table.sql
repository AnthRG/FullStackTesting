CREATE TABLE notifications (
    id            BIGSERIAL PRIMARY KEY,
    type          VARCHAR(30)  NOT NULL,
    product_id    BIGINT       NOT NULL,
    product_name  VARCHAR(150) NOT NULL,
    product_sku   VARCHAR(50)  NOT NULL,
    quantity      INTEGER      NOT NULL,
    minimum_stock INTEGER      NOT NULL,
    message       TEXT         NOT NULL,
    read          BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at       TIMESTAMP,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),

    -- ON DELETE CASCADE: las notificaciones son alertas operativas desechables,
    -- no historial. A diferencia de stock_movements, no deben impedir borrar un producto.
    CONSTRAINT fk_notifications_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT chk_notification_type     CHECK (type IN ('LOW_STOCK', 'OUT_OF_STOCK')),
    CONSTRAINT chk_notification_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_notification_minimum  CHECK (minimum_stock >= 0),
    CONSTRAINT chk_notification_read_at  CHECK (read = (read_at IS NOT NULL))
);

CREATE INDEX idx_notifications_created_at ON notifications (created_at);
CREATE INDEX idx_notifications_product ON notifications (product_id);
