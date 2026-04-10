# 电商项目分模块数据表设计

版本：2026-04-10

## 1. 文档目标

本文档基于当前项目的模块划分，对各服务所需的数据表进行第一版设计讨论，目标是：

- 明确每个服务各自拥有的数据
- 避免跨服务直接共享数据库表
- 支持当前项目规划中的核心业务流程
- 为后续 Flyway migration 和实体建模提供依据

当前项目模块见：

- `auth-service`
- `product-service`
- `cart-service`
- `order-service`
- `inventory-listener`
- `payment-service`
- `gateway-service`
- `libs/common`
- `frontend`

其中：

- `gateway-service` 不需要独立业务表
- `frontend` 不需要独立业务表
- `libs/common` 是共享 DTO / 消息模型，不需要独立业务表

## 2. 设计原则

### 2.1 服务自治

每个服务只管理自己的数据表，不直接依赖其他服务数据库中的表。

例如：

- 订单服务不直接 join 商品服务的商品表
- 支付服务不直接修改订单服务的订单表
- 认证服务不直接保存商品或订单信息

### 2.2 跨服务传 ID 和快照

跨服务只传必要标识和快照信息，不做跨库外键。

例如：

- `order_items` 中保存商品名称、价格、图片快照
- 支付服务只保存 `order_id`、金额、支付状态

### 2.3 交易数据优先保留快照

订单、支付、库存等交易型数据应保留创建当时的业务快照，避免后续商品改价、改名、下架后影响历史数据展示。

### 2.4 幂等与可追踪

对于下单、库存扣减、支付回调、消息消费等关键动作，需要保留：

- 幂等键
- 消费记录
- 状态流转记录
- 业务流水

## 3. 核心业务流程对应的数据需求

项目当前规划支持如下流程：

1. 用户注册、登录
2. 商品浏览
3. 加入购物车
4. 提交订单
5. 发送 `order.created`
6. 监听库存预扣
7. 模拟支付
8. 发送 `order.paid`
9. 更新订单状态

因此第一版数据设计至少要覆盖：

- 用户与认证
- 商品与库存
- 购物车
- 订单与订单项
- 库存预扣记录
- 支付单与支付回调
- 消息幂等与事件投递

## 4. 按服务划分的数据表设计

## 4.1 auth-service

### 职责

- 用户注册
- 用户登录
- 刷新 Token
- 用户基础身份信息管理

### 建议表

#### `users`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 用户 ID |
| username | VARCHAR(64) | 用户名 |
| email | VARCHAR(128) | 邮箱 |
| password_hash | VARCHAR(255) | 加密后的密码 |
| status | VARCHAR(32) | 用户状态，例如 ACTIVE / DISABLED |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

约束与索引建议：

- `username` 唯一
- `email` 唯一
- `status` 可建立普通索引，便于后台筛选

#### `user_addresses`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 地址 ID |
| user_id | BIGINT | 用户 ID |
| receiver_name | VARCHAR(64) | 收货人姓名 |
| receiver_phone | VARCHAR(32) | 收货电话 |
| province | VARCHAR(64) | 省 |
| city | VARCHAR(64) | 市 |
| district | VARCHAR(64) | 区 |
| detail_address | VARCHAR(255) | 详细地址 |
| is_default | TINYINT(1) | 是否默认地址 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

约束与索引建议：

- `INDEX(user_id)`
- 可增加 `(user_id, is_default)` 组合索引

#### `refresh_tokens`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| user_id | BIGINT | 用户 ID |
| token_hash | VARCHAR(255) | refresh token 哈希值 |
| expires_at | DATETIME | 过期时间 |
| revoked_at | DATETIME NULL | 撤销时间 |
| created_at | DATETIME | 创建时间 |

约束与索引建议：

- `token_hash` 唯一
- `INDEX(user_id)`

### 说明

- 不建议直接保存明文 token
- 建议只保存 refresh token，access token 仍使用 JWT 无状态模式

## 4.2 product-service

### 职责

- 商品展示
- 商品分类管理
- SKU 管理
- 商品库存主数据维护

### 建议表

#### `product_categories`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 分类 ID |
| name | VARCHAR(64) | 分类名 |
| parent_id | BIGINT NULL | 父分类 ID |
| sort_order | INT | 排序值 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

#### `products`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 商品 ID |
| name | VARCHAR(128) | 商品名称 |
| slug | VARCHAR(128) | URL 标识 |
| category_id | BIGINT | 分类 ID |
| brand | VARCHAR(64) NULL | 品牌 |
| description | TEXT | 商品描述 |
| cover_image | VARCHAR(255) NULL | 封面图 |
| status | VARCHAR(32) | 商品状态，例如 DRAFT / ON_SALE / OFF_SHELF |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

约束与索引建议：

- `slug` 唯一
- `INDEX(category_id, status)`

#### `product_skus`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | SKU ID |
| product_id | BIGINT | 商品 ID |
| sku_code | VARCHAR(64) | SKU 编码 |
| spec_json | JSON | 规格信息 |
| sale_price | DECIMAL(10,2) | 售价 |
| market_price | DECIMAL(10,2) NULL | 划线价 |
| stock | INT | 可用库存 |
| locked_stock | INT | 锁定库存 |
| status | VARCHAR(32) | SKU 状态 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

约束与索引建议：

- `sku_code` 唯一
- `INDEX(product_id)`

#### `product_images`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| product_id | BIGINT | 商品 ID |
| image_url | VARCHAR(255) | 图片地址 |
| sort_order | INT | 排序 |

### 说明

- 建议尽早按 SKU 级维护库存，不要只在商品级存库存
- `locked_stock` 用于后续库存预占和并发处理

## 4.3 cart-service

### 职责

- 保存用户购物车
- 修改购买数量
- 删除购物车项
- 选中 / 取消选中

### 存储建议

当前阶段优先使用 Redis，不强制落 MySQL。

### Redis Key 设计

#### `cart:{userId}`

建议 value 保存为 JSON 或 Hash，包含字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| skuId | BIGINT | SKU ID |
| productId | BIGINT | 商品 ID |
| productName | VARCHAR | 商品名快照 |
| coverImage | VARCHAR | 封面图快照 |
| unitPrice | DECIMAL | 加入购物车时价格快照 |
| quantity | INT | 购买数量 |
| selected | BOOLEAN | 是否选中 |
| updatedAt | DATETIME | 更新时间 |

### 说明

- 购物车是临时态数据，优先 Redis 更合适
- 下单时由订单服务读取购物车快照并持久化为正式订单
- 如果后续需要“购物车历史恢复”，再考虑补充 MySQL 归档表

## 4.4 order-service

### 职责

- 创建订单
- 保存订单快照
- 管理订单状态
- 发布订单事件
- 处理支付完成后的订单状态推进

### 建议表

#### `orders`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 订单 ID |
| order_no | VARCHAR(64) | 订单号 |
| user_id | BIGINT | 用户 ID |
| status | VARCHAR(32) | 订单状态，例如 CREATED / PAID / CANCELLED |
| total_amount | DECIMAL(10,2) | 商品总金额 |
| payable_amount | DECIMAL(10,2) | 应付金额 |
| payment_status | VARCHAR(32) | 支付状态 |
| delivery_status | VARCHAR(32) | 发货状态 |
| idempotency_key | VARCHAR(64) | 幂等键 |
| remark | VARCHAR(255) NULL | 订单备注 |
| receiver_name | VARCHAR(64) | 收货人 |
| receiver_phone | VARCHAR(32) | 收货电话 |
| receiver_address_snapshot | VARCHAR(255) | 收货地址快照 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

约束与索引建议：

- `order_no` 唯一
- `idempotency_key` 唯一
- `INDEX(user_id, created_at)`

#### `order_items`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| order_id | BIGINT | 订单 ID |
| product_id | BIGINT | 商品 ID |
| sku_id | BIGINT | SKU ID |
| product_name_snapshot | VARCHAR(128) | 商品名快照 |
| sku_desc_snapshot | VARCHAR(255) NULL | SKU 规格快照 |
| cover_image_snapshot | VARCHAR(255) NULL | 图片快照 |
| unit_price | DECIMAL(10,2) | 下单单价 |
| quantity | INT | 数量 |
| line_total | DECIMAL(10,2) | 小计 |

约束与索引建议：

- `INDEX(order_id)`

#### `order_status_logs`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| order_id | BIGINT | 订单 ID |
| from_status | VARCHAR(32) | 原状态 |
| to_status | VARCHAR(32) | 新状态 |
| operator | VARCHAR(64) | 操作人或系统标识 |
| remark | VARCHAR(255) NULL | 备注 |
| created_at | DATETIME | 创建时间 |

#### `order_outbox_events`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| aggregate_type | VARCHAR(32) | 聚合类型，例如 ORDER |
| aggregate_id | BIGINT | 聚合主键 |
| event_type | VARCHAR(64) | 事件类型，例如 order.created |
| payload_json | JSON | 事件内容 |
| status | VARCHAR(32) | 发送状态，例如 NEW / SENT / FAILED |
| retry_count | INT | 重试次数 |
| created_at | DATETIME | 创建时间 |
| sent_at | DATETIME NULL | 发送时间 |

### 说明

- `order_items` 必须保存商品快照，不能实时查商品表
- `idempotency_key` 是避免重复下单的关键字段
- 强烈建议使用 Outbox 模式保证“订单落库”和“事件发送”最终一致

## 4.5 inventory-listener

### 职责

- 消费 `order.created`
- 执行库存预扣
- 记录库存事务
- 发布扣减成功或补偿事件
- 防止消息重复消费

### 建议表

#### `inventory_reservations`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| order_id | BIGINT | 订单 ID |
| user_id | BIGINT | 用户 ID |
| status | VARCHAR(32) | 状态，例如 RESERVED / FAILED / RELEASED |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

约束与索引建议：

- `INDEX(order_id)`
- 可对 `order_id` 建唯一索引，保证一个订单只产生一条预扣主记录

#### `inventory_reservation_items`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| reservation_id | BIGINT | 预扣主表 ID |
| product_id | BIGINT | 商品 ID |
| sku_id | BIGINT | SKU ID |
| quantity | INT | 预扣数量 |

#### `inventory_tx_logs`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| order_id | BIGINT | 订单 ID |
| sku_id | BIGINT | SKU ID |
| tx_type | VARCHAR(32) | 事务类型，例如 RESERVE / CONFIRM / RELEASE |
| quantity | INT | 数量 |
| before_stock | INT | 变更前库存 |
| after_stock | INT | 变更后库存 |
| message_id | VARCHAR(64) | 对应消息 ID |
| created_at | DATETIME | 创建时间 |

#### `consumed_messages`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| message_id | VARCHAR(64) | 消息 ID |
| consumer_name | VARCHAR(64) | 消费者名称 |
| event_type | VARCHAR(64) | 事件类型 |
| consumed_at | DATETIME | 消费时间 |

约束与索引建议：

- `(message_id, consumer_name)` 唯一

### 说明

- 即使库存主数据放在 `product-service`，这里也建议保留预扣记录和消费记录
- 没有这些记录会导致后续排障、补偿、幂等处理非常困难

## 4.6 payment-service

### 职责

- 创建支付单
- 模拟支付
- 接收支付回调
- 发布 `order.paid`

### 建议表

#### `payment_orders`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| payment_no | VARCHAR(64) | 支付单号 |
| order_id | BIGINT | 订单 ID |
| user_id | BIGINT | 用户 ID |
| amount | DECIMAL(10,2) | 支付金额 |
| payment_method | VARCHAR(32) | 支付方式 |
| status | VARCHAR(32) | 状态，例如 INIT / SUCCESS / FAILED |
| third_party_txn_id | VARCHAR(64) NULL | 第三方交易流水号 |
| created_at | DATETIME | 创建时间 |
| paid_at | DATETIME NULL | 支付时间 |
| updated_at | DATETIME | 更新时间 |

约束与索引建议：

- `payment_no` 唯一
- `INDEX(order_id)`
- `third_party_txn_id` 可设唯一

#### `payment_callbacks`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| payment_order_id | BIGINT | 支付单 ID |
| callback_body | JSON | 回调原始内容 |
| callback_status | VARCHAR(32) | 回调处理状态 |
| signature_valid | TINYINT(1) | 签名是否通过 |
| received_at | DATETIME | 接收时间 |

#### `payment_outbox_events`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| event_type | VARCHAR(64) | 事件类型，例如 order.paid |
| aggregate_id | BIGINT | 业务聚合主键 |
| payload_json | JSON | 事件内容 |
| status | VARCHAR(32) | 发送状态 |
| retry_count | INT | 重试次数 |
| created_at | DATETIME | 创建时间 |

### 说明

- 支付回调需要天然幂等
- 即使是 mock 支付，也建议保留回调表，后续真实接支付网关时可平滑演进

## 5. 不需要单独建业务表的模块

## 5.1 gateway-service

网关主要负责：

- 路由转发
- JWT 校验
- request-id 注入
- 限流和日志

当前阶段通常不需要业务表。

如果后续做动态路由、黑白名单、限流规则，可考虑额外设计：

- `gateway_route_configs`
- `gateway_access_rules`

但 MVP 阶段不建议先上。

## 5.2 frontend

前端不需要单独业务表，主要通过 API 消费后端服务。

## 5.3 libs/common

共享库不需要建表，主要沉淀：

- DTO
- 事件模型
- 常量枚举

## 6. 第一版 MVP 最小建表集合

如果当前目标是尽快把项目跑通，建议优先落以下最小表集合：

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

同时使用：

- Redis 保存购物车
- RabbitMQ 传递领域事件

这样已经足以支撑：

- 注册 / 登录
- 商品浏览
- 购物车
- 下单
- 库存预扣
- 支付
- 订单状态推进

## 7. 推荐状态枚举

### 7.1 用户状态

- `ACTIVE`
- `DISABLED`

### 7.2 商品状态

- `DRAFT`
- `ON_SALE`
- `OFF_SHELF`

### 7.3 订单状态

- `CREATED`
- `PAID`
- `CANCELLED`
- `SHIPPED`
- `COMPLETED`

### 7.4 支付状态

- `INIT`
- `SUCCESS`
- `FAILED`
- `CLOSED`

### 7.5 库存预扣状态

- `RESERVED`
- `FAILED`
- `RELEASED`

### 7.6 Outbox 状态

- `NEW`
- `SENT`
- `FAILED`

## 8. 关键设计提醒

### 8.1 不要跨服务做数据库外键

例如：

- `orders.user_id` 不要直接外键关联 `users.id`
- `order_items.product_id` 不要直接外键关联商品库

原因是微服务边界下，不同服务数据库应保持独立。

### 8.2 订单项必须保留快照

必须保留：

- 商品名称
- SKU 规格
- 下单价格
- 商品图片

否则商品变更后历史订单会失真。

### 8.3 库存必须有流水和幂等记录

如果只有库存值，没有事务日志和消费记录，后续很难排查：

- 是否重复扣减
- 是否漏补偿
- 是否某条消息重复消费

### 8.4 购物车和订单不要混用模型

购物车是临时态，订单是交易凭证，两者生命周期和数据约束不同，建议分开设计。

### 8.5 下单和事件发送建议采用 Outbox

否则会出现典型问题：

- 订单已写库，但消息没发出
- 消息发出了，但订单事务回滚了

## 9. 下一步落地建议

建议后续开发按以下顺序推进：

1. 为 `auth-service`、`product-service`、`order-service`、`payment-service`、`inventory-listener` 分别规划独立 schema 或独立 migration 目录
2. 先写 MVP 版本的 Flyway SQL
3. 先实现最小实体与 Repository
4. 再接入下单、库存、支付事件流
5. 最后补充状态日志、Outbox、消费幂等表

## 10. 建议的第一批 Flyway 迁移范围

推荐第一批 migration 只覆盖：

- `users`
- `refresh_tokens`
- `products`
- `product_skus`
- `orders`
- `order_items`
- `payment_orders`
- `inventory_reservations`
- `consumed_messages`

第二批再补：

- `user_addresses`
- `product_categories`
- `product_images`
- `order_status_logs`
- `order_outbox_events`
- `inventory_reservation_items`
- `inventory_tx_logs`
- `payment_callbacks`
- `payment_outbox_events`

---

如果后续要继续落地实现，建议下一步直接输出一版：

- MySQL 建表 SQL
- Flyway migration 文件规划
- Java Entity / Repository 对应关系
