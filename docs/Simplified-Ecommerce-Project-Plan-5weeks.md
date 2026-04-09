# 简化电商系统 — 5 周可执行计划
版本：2026-04-09

技术栈（推荐）
- Java 21、Spring Boot 3.x、Gradle (Kotlin DSL)
- RabbitMQ（消息）、Redis 7（缓存/购物车/预扣）、MySQL 8（持久化）、Flyway（迁移）
- 前端：React 18 + Vite + TypeScript、React Router、Ant Design（或 MUI）
- 测试：JUnit5、Mockito、Testcontainers
- 可观测：Micrometer -> Prometheus、OpenTelemetry/Jaeger
- CI：GitHub Actions（仅 build & test）

一行目标
- 在 5 周内把“注册/登录、商品浏览、购物车、下单（含库存预扣）、模拟支付、订单状态”做成可演示系统；并包含消息契约、docker-compose 启动脚本、基础 CI 与集成测试 scaffold。

高层架构（简述）
- Gateway（Spring Cloud Gateway）负责路由与鉴权校验
- Auth Service（JWT）
- Product Service（商品元数据 + stock）
- Cart Service（Redis 存储）
- Order Service（创建订单 + 发布 order.created）
- Inventory Listener（监听 order.created，Redis 预扣 + 持久化更新）
- Payment Service（模拟支付 + 发布 order.paid）
- libs/common（共享 DTO/消息模型）
- frontend（React）
- infra（docker-compose.dev.yml, Prometheus, Jaeger）

事件流（最小）
1. 客户端 -> order-service POST /api/orders（包含 idempotencyKey）
2. order-service 持久化订单（CREATED）并发布 `order.created` 到 RabbitMQ
3. inventory-listener 订阅 `order.created`，执行 Redis 原子预扣（Lua），成功 -> 更新 DB（减 stock）并可发布 `inventory.confirm`；失败 -> 发布 `inventory.compensate`（通知 order-service 回滚或人工介入）
4. client -> payment-service/pay 模拟支付 -> payment 回调发布 `order.paid` -> order-service 订阅并将订单置为 PAID

RabbitMQ 设计（建议）
- Exchanges（type=topic 或 direct，durable）
    - exchange.order
    - exchange.inventory
    - exchange.payment
- Queues（durable）
    - queue.order.created
    - queue.inventory.deduct
    - queue.inventory.compensate
    - queue.payment.callback
- DLX / Retry
    - 每个工作队列绑定 DLX，失败消息走 DLX 到 retry 队列（带 TTL），多次失败进入人工队列

示例消息（JSON）
- order.created
```json
{
  "messageId": "uuid-v4",
  "timestamp": "2026-04-09T12:00:00Z",
  "event": "order.created",
  "payload": {
    "orderId": 12345,
    "userId": 678,
    "items": [
      {"productId": 101, "qty": 2, "unitPrice": 99.9}
    ],
    "totalAmount": 199.8,
    "idempotencyKey": "client-key-xxx"
  }
}