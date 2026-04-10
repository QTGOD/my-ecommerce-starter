CREATE TABLE inventory_reservations (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'RESERVED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_inventory_reservations_order_id UNIQUE (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE consumed_messages (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    message_id VARCHAR(64) NOT NULL,
    consumer_name VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    consumed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_consumed_messages_msg_consumer UNIQUE (message_id, consumer_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE inventory_reservation_items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    INDEX idx_inventory_reservation_items_reservation_id (reservation_id),
    INDEX idx_inventory_reservation_items_sku_id (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE inventory_tx_logs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    tx_type VARCHAR(32) NOT NULL,
    quantity INT NOT NULL,
    before_stock INT NOT NULL,
    after_stock INT NOT NULL,
    message_id VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_inventory_tx_logs_order_id (order_id),
    INDEX idx_inventory_tx_logs_sku_id (sku_id),
    INDEX idx_inventory_tx_logs_message_id (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
