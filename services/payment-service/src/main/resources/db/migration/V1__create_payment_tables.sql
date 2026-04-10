CREATE TABLE payment_orders (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    payment_no VARCHAR(64) NOT NULL,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'INIT',
    third_party_txn_id VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at DATETIME NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_payment_orders_payment_no UNIQUE (payment_no),
    CONSTRAINT uk_payment_orders_third_party_txn_id UNIQUE (third_party_txn_id),
    INDEX idx_payment_orders_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payment_callbacks (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    payment_order_id BIGINT NOT NULL,
    callback_body JSON NOT NULL,
    callback_status VARCHAR(32) NOT NULL DEFAULT 'RECEIVED',
    signature_valid TINYINT(1) NOT NULL DEFAULT 0,
    received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_payment_callbacks_payment_order_id (payment_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payment_outbox_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    payload_json JSON NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'NEW',
    retry_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_payment_outbox_status_created_at (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
