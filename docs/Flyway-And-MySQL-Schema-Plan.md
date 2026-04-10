# Flyway 迁移规划与 MySQL 建表草案

版本：2026-04-10

## 1. 文档目标

本文档在 [Database-Design-By-Service.md](D:/works/AI/my-ecommerce-starter/docs/Database-Design-By-Service.md) 的基础上，进一步给出：

- 按服务拆分的 Flyway 迁移目录规划
- 第一版 MySQL 建表 SQL 草案
- 推荐的表创建顺序
- Java Entity 字段映射建议

目标是让当前项目可以从“表设计讨论”推进到“逐步落地实现”。

## 2. 数据库拆分建议

建议每个核心服务拥有独立 schema。这样更符合后续微服务演进方向，也便于隔离权限和迁移管理。

推荐 schema：

- `shop_auth`
- `shop_product`
- `shop_order`
- `shop_inventory`
- `shop_payment`

当前阶段如果为了本地开发简单，也可以先只用一个数据库 `shop`，但仍建议通过不同服务各自的 Flyway migration 目录来保持边界。

## 3. Flyway 目录规划

建议每个服务使用自己的 migration 目录。

推荐目录结构：

```text
services/
  auth-service/
    src/main/resources/db/migration/
      V1__create_users.sql
      V2__create_refresh_tokens.sql
      V3__create_user_addresses.sql
  product-service/
    src/main/resources/db/migration/
      V1__create_product_categories.sql
      V2__create_products.sql
      V3__create_product_skus.sql
      V4__create_product_images.sql
  order-service/
    src/main/resources/db/migration/
      V1__create_orders.sql
      V2__create_order_items.sql
      V3__create_order_status_logs.sql
      V4__create_order_outbox_events.sql
  inventory-listener/
    src/main/resources/db/migration/
      V1__create_inventory_reservations.sql
      V2__create_inventory_reservation_items.sql
      V3__create_inventory_tx_logs.sql
      V4__create_consumed_messages.sql
  payment-service/
    src/main/resources/db/migration/
      V1__create_payment_orders.sql
      V2__create_payment_callbacks.sql
      V3__create_payment_outbox_events.sql
```

## 4. 第一版 MVP 推荐迁移范围

建议先只落以下表：

- `users`
- `refresh_tokens`
- `products`
- `product_skus`
- `orders`
- `order_items`
- `order_outbox_events`
- `inventory_reservations`
- `consumed_messages`
- `payment_orders`

第二批再补：

- `user_addresses`
- `product_categories`
- `product_images`
- `order_status_logs`
- `inventory_reservation_items`
- `inventory_tx_logs`
- `payment_callbacks`
- `payment_outbox_events`

## 5. MySQL 建表 SQL 草案

以下 SQL 以 MySQL 8 为基准，字符集建议统一使用 `utf8mb4`。

## 5.1 auth-service

### `users`

```sql
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    email VARCHAR(128) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### `refresh_tokens`

```sql
CREATE TABLE refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    INDEX idx_refresh_tokens_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### `user_addresses`

```sql
CREATE TABLE user_addresses (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    receiver_name VARCHAR(64) NOT NULL,
    receiver_phone VARCHAR(32) NOT NULL,
    province VARCHAR(64) NOT NULL,
    city VARCHAR(64) NOT NULL,
    district VARCHAR(64) NOT NULL,
    detail_address VARCHAR(255) NOT NULL,
    is_default TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_addresses_user_id (user_id),
    INDEX idx_user_addresses_user_default (user_id, is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 5.2 product-service

### `product_categories`

```sql
CREATE TABLE product_categories (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    parent_id BIGINT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_product_categories_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### `products`

```sql
CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    slug VARCHAR(128) NOT NULL,
    category_id BIGINT NOT NULL,
    brand VARCHAR(64) NULL,
    description TEXT NOT NULL,
    cover_image VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ON_SALE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_products_slug UNIQUE (slug),
    INDEX idx_products_category_status (category_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### `product_skus`

```sql
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
```

### `product_images`

```sql
CREATE TABLE product_images (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    INDEX idx_product_images_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 5.3 order-service

### `orders`

```sql
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
```

### `order_items`

```sql
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
```

### `order_status_logs`

```sql
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
```

### `order_outbox_events`

```sql
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
    INDEX idx_order_outbox_status_created_at (status, created_at),
    INDEX idx_order_outbox_aggregate (aggregate_type, aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 5.4 inventory-listener

### `inventory_reservations`

```sql
CREATE TABLE inventory_reservations (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'RESERVED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_inventory_reservations_order_id UNIQUE (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### `inventory_reservation_items`

```sql
CREATE TABLE inventory_reservation_items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    INDEX idx_inventory_reservation_items_reservation_id (reservation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### `inventory_tx_logs`

```sql
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
```

### `consumed_messages`

```sql
CREATE TABLE consumed_messages (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    message_id VARCHAR(64) NOT NULL,
    consumer_name VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    consumed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_consumed_messages_msg_consumer UNIQUE (message_id, consumer_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 5.5 payment-service

### `payment_orders`

```sql
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
```

### `payment_callbacks`

```sql
CREATE TABLE payment_callbacks (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    payment_order_id BIGINT NOT NULL,
    callback_body JSON NOT NULL,
    callback_status VARCHAR(32) NOT NULL DEFAULT 'RECEIVED',
    signature_valid TINYINT(1) NOT NULL DEFAULT 0,
    received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_payment_callbacks_payment_order_id (payment_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### `payment_outbox_events`

```sql
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
```

## 6. 推荐的 Flyway 首批文件内容映射

如果你要快速开始，我建议第一批就创建以下文件：

### auth-service

- `V1__create_users.sql`
- `V2__create_refresh_tokens.sql`

### product-service

- `V1__create_products.sql`
- `V2__create_product_skus.sql`

### order-service

- `V1__create_orders.sql`
- `V2__create_order_items.sql`
- `V3__create_order_outbox_events.sql`

### inventory-listener

- `V1__create_inventory_reservations.sql`
- `V2__create_consumed_messages.sql`

### payment-service

- `V1__create_payment_orders.sql`

## 7. Java Entity 字段映射建议

这里不是完整代码，只给第一版字段命名建议，方便你后续写实体类。

### `users`

```java
Long id;
String username;
String email;
String passwordHash;
String status;
LocalDateTime createdAt;
LocalDateTime updatedAt;
```

### `products`

```java
Long id;
String name;
String slug;
Long categoryId;
String brand;
String description;
String coverImage;
String status;
LocalDateTime createdAt;
LocalDateTime updatedAt;
```

### `product_skus`

```java
Long id;
Long productId;
String skuCode;
String specJson;
BigDecimal salePrice;
BigDecimal marketPrice;
Integer stock;
Integer lockedStock;
String status;
LocalDateTime createdAt;
LocalDateTime updatedAt;
```

### `orders`

```java
Long id;
String orderNo;
Long userId;
String status;
BigDecimal totalAmount;
BigDecimal payableAmount;
String paymentStatus;
String deliveryStatus;
String idempotencyKey;
String remark;
String receiverName;
String receiverPhone;
String receiverAddressSnapshot;
LocalDateTime createdAt;
LocalDateTime updatedAt;
```

### `order_items`

```java
Long id;
Long orderId;
Long productId;
Long skuId;
String productNameSnapshot;
String skuDescSnapshot;
String coverImageSnapshot;
BigDecimal unitPrice;
Integer quantity;
BigDecimal lineTotal;
```

### `payment_orders`

```java
Long id;
String paymentNo;
Long orderId;
Long userId;
BigDecimal amount;
String paymentMethod;
String status;
String thirdPartyTxnId;
LocalDateTime createdAt;
LocalDateTime paidAt;
LocalDateTime updatedAt;
```

## 8. 建议的实现顺序

推荐按下面的顺序落地，阻力最小：

1. `auth-service` 上 `users`、`refresh_tokens`
2. `product-service` 上 `products`、`product_skus`
3. `order-service` 上 `orders`、`order_items`
4. `payment-service` 上 `payment_orders`
5. `inventory-listener` 上 `inventory_reservations`、`consumed_messages`
6. 第二轮再补日志表、回调表、Outbox 细化表

## 9. 实施注意点

### 9.1 当前阶段可不加外键

因为项目是微服务架构方向，建议先不做跨服务外键。

即使在同一个 MySQL 实例里，也不建议让：

- `order_items.product_id` 外键到 `products.id`
- `orders.user_id` 外键到 `users.id`

### 9.2 金额字段统一用 `DECIMAL(10,2)`

不要用 `float` 或 `double` 存金额。

### 9.3 状态字段先用字符串

先用 `VARCHAR(32)` 比直接用 MySQL `ENUM` 更灵活，后续 Java 侧再映射成枚举类。

### 9.4 主键统一使用 `BIGINT`

当前阶段最简单，方便和 Java `Long` 对齐。

### 9.5 JSON 字段只存变化快、结构不稳定的内容

例如：

- `spec_json`
- `payload_json`
- `callback_body`

核心查询字段仍应放在普通列中。

## 10. 后续建议

接下来最适合继续推进的有两件事：

1. 直接为每个服务生成对应的 Flyway SQL 文件
2. 为 `product-service`、`order-service`、`payment-service` 生成第一版 Entity / Repository 骨架

如果你希望，我下一步可以继续直接帮你把这些 SQL 文件落到各个服务的 `src/main/resources/db/migration/` 目录里。
