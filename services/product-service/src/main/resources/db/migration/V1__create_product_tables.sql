CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    slug VARCHAR(128) NOT NULL,
    category_id BIGINT NOT NULL DEFAULT 0,
    brand VARCHAR(64) NULL,
    description TEXT NOT NULL,
    cover_image VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ON_SALE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_products_slug UNIQUE (slug),
    INDEX idx_products_category_status (category_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE product_skus (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    sku_code VARCHAR(64) NOT NULL,
    spec_json JSON NOT NULL,
    sale_price DECIMAL(10,2) NOT NULL,
    market_price DECIMAL(10,2) NULL,
    stock INT NOT NULL DEFAULT 0,
    locked_stock INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ON_SALE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_product_skus_sku_code UNIQUE (sku_code),
    INDEX idx_product_skus_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE product_categories (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    parent_id BIGINT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_product_categories_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE product_images (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    INDEX idx_product_images_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
