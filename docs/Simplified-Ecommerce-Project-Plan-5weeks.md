# 简化电商系统 — 5 周可执行计划
版本：2026-04-09
技术栈：Java 21, Spring Boot 3.x, Gradle (Kotlin DSL), RabbitMQ, Redis 7, MySQL 8, React (Vite + TS)

一行概要
构建简化电商（用户/商品/购物车/下单/支付模拟），采用微服务 + RabbitMQ 事件驱动。此文档为压缩到 5 周（包含周末可选加速）。
（此文件为精简版，更多细节见 Detailed-5-Week-Daily-Plan.md）

交付目标（5 周）
- 完整最小可演示系统：注册/登录、浏览商品、购物车、下单、库存预扣、模拟支付、订单状态展示
- 基本监控与链路追踪（Prometheus + Jaeger）
- CI（Gradle build + tests），Docker Compose 启动示例

周计划简介
Week 1: 项目准备、mono-repo 骨架、infra 启动、auth + gateway
Week 2: product 服务、前端基础（React）、购物车后端设计
Week 3: cart 服务、order 服务同步下单、消息契约定义、RabbitMQ 集成
Week 4: inventory-listener（Redis 预扣 + 重试/DLX）、payment 模拟、订单状态机
Week 5: Observability（metrics/tracing）、集成测试（Testcontainers）、压测与优化、CI完善与 demo 准备

运行：请参见仓库根 README.md 的快速启动节
