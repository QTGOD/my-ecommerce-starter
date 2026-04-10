CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
    total_amount DECIMAL(10,2) NOT NULL,
    payable_amount DECIMAL(10,2) NOT NULL,
    payment_status VARCHAR(32) NOT NULL DEFAULT 'INIT',
    delivery_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    idempotency_key VARCHAR(64) NOT NULL,
    remark VARCHAR(255) NULL,
    receiver_name VARCHAR(64) NOT NULL,
    receiver_phone VARCHAR(32) NOT NULL,
    receiver_address_snapshot VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_orders_order_no UNIQUE (order_no),
    CONSTRAINT uk_orders_idempotency_key UNIQUE (idempotency_key),
    INDEX idx_orders_user_id_created_at (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    product_name_snapshot VARCHAR(128) NOT NULL,
    sku_desc_snapshot VARCHAR(255) NULL,
    cover_image_snapshot VARCHAR(255) NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    line_total DECIMAL(10,2) NOT NULL,
    INDEX idx_order_items_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_outbox_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    aggregate_type VARCHAR(32) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload_json JSON NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'NEW',
    retry_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at DATETIME NULL,
    INDEX idx_order_outbox_status_created_at (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_status_logs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    from_status VARCHAR(32) NOT NULL,
    to_status VARCHAR(32) NOT NULL,
    operator VARCHAR(64) NOT NULL,
    remark VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_status_logs_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
