# 全量 MySQL 表清单

版本：2026-04-23

当前项目内已经补齐的建表 SQL 如下。

当前约定：

- 每个模块只保留 `1` 个可执行 SQL 脚本
- 所有表都合并在各模块自己的 `V1__*.sql` 中

## auth-service

- `users`
- `refresh_tokens`
- `user_addresses`

对应目录：

- [auth migration dir](D:/works/AI/my-ecommerce-starter/services/auth-service/src/main/resources/db/migration)

## product-service

- `products`
- `product_skus`
- `product_categories`
- `product_images`

对应目录：

- [product migration dir](D:/works/AI/my-ecommerce-starter/services/product-service/src/main/resources/db/migration)

## order-service

- `orders`
- `order_items`
- `order_outbox_events`
- `order_status_logs`

对应目录：

- [order migration dir](D:/works/AI/my-ecommerce-starter/services/order-service/src/main/resources/db/migration)

## inventory-listener

- `inventory_reservations`
- `consumed_messages`
- `inventory_reservation_items`
- `inventory_tx_logs`

对应目录：

- [inventory migration dir](D:/works/AI/my-ecommerce-starter/services/inventory-listener/src/main/resources/db/migration)

## payment-service

- `payment_orders`
- `payment_callbacks`
- `payment_outbox_events`

对应目录：

- [payment migration dir](D:/works/AI/my-ecommerce-starter/services/payment-service/src/main/resources/db/migration)

## 不需要建表的模块

- `gateway-service`
- `frontend`
- `libs/common`
- `cart-service`

说明：

- `cart-service` 当前阶段优先走 Redis / 内存方案，没有新增 MySQL 表。
- 如果后续要做购物车持久化，可以再补 `cart_items` 或归档表。
