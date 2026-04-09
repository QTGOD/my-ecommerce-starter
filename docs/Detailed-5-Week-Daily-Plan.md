
```markdown name=docs/Detailed-5-Week-Daily-Plan.md
# 逐日 5 周执行计划（含周末，加速版）
版本：2026-04-09

说明：这份日程面向“你现在有充裕时间、愿意在周末也开发”的情况。每日按 8 小时工作量安排（含编码、学习与缓冲时间）。前端为初学者，前期留出较多学习时间。若感到节奏太紧可将周末用作缓冲/学习日。

每日惯例（建议）
- 09:00–09:30：拉代码、看 Issues、定今日目标  
- 09:30–12:30：深度编码（3h）  
- 12:30–13:30：午休  
- 13:30–16:00：实现/学习（2.5h）  
- 16:00–17:30：联调/测试/日志分析（1.5h）  
- 17:30–18:00：日结（写 README、更新 issue、commit）

总体时间线（压缩 5 周）
- Week 1（Day1–Day7）：准备与骨架（mono-repo, infra）、Auth + Gateway、Flyway  
- Week 2（Day8–Day14）：Product Service、frontend 初始化、Product 前后端联调  
- Week 3（Day15–Day21）：Cart Service（Redis）、Order Service 同步下单、RabbitMQ 集成与消息契约  
- Week 4（Day22–Day28）：Inventory Listener（Redis Lua 预扣 + DB 持久��）、Payment 模拟、订单状态机  
- Week 5（Day29–Day35）：Observability（metrics/tracing）、集成测试、压测、CI、Dockerfile 与最终文档 + 演示准备

下面按天列出具体任务（每天包含产出与验收标准）：

—— Week 1：准备与骨架（Day 1 ~ Day 7） ——
Day 1（项目初始化）
- 任务：创建仓库目录结构、settings.gradle.kts 与父 build、创建 docs 占位文件
- 学习：Gradle Kotlin DSL 入门（1h）
- 产出：项目骨架，README 初稿
- 验收：能够在 IDE 打开 multi-module 项目

Day 2（infra 与 Docker Compose）
- 任务：编写 infra/docker-compose.dev.yml（MySQL/Redis/RabbitMQ/Jaeger/Prometheus），启动并验证容器健康
- 学习：Docker Compose 基本命令（0.5h）
- 产出：可启动 infra 的 docker compose 文件
- 验收：docker compose up 后 mysql/redis/rabbitmq 可访问

Day 3（common-lib 与模块脚手架）
- 任务：创建 libs/common（DTO、消息模型占位）、各服务 module skeleton（gateway/auth/product/cart/order/inventory/payment）
- 产出：每个模块都有 Application 类与简单 controller（/hello）
- 验收：能运行 ./gradlew build（跳过测试）

Day 4（Flyway & DB schema）
- 任务：为 auth/product/order 添加 Flyway migration（users/products/orders/order_items）
- 学习：Flyway migration 基本用法（0.5h）
- 产出：初始 SQL 文件
- 验收：容器中的 MySQL 执行 migration（或本地检查 SQL）

Day 5（auth-service：注册/登录）
- 任务：实现 auth-service 的注册/登录（BCrypt，JWT 签发 + refresh token）接口
- 学习：JWT 原理（0.5h）
- 产出：/api/auth/register, /api/auth/login, /api/auth/refresh
- 验收：能使用 Postman 注册并登录得到 access token

Day 6（gateway-service：路由与 JWT 过滤）
- 任务：实现 gateway 路由到服务并加入 JWT 校验过滤器、request-id
- 学习：Spring Cloud Gateway filter（0.5h）
- 产出：网关能验证 token 并代理到 product/service
- 验收：通过 gateway 调用受保护 API 得到 401/200 的正确行为

Day 7（周末加速：本周整合 + demo）
- 任务：整合已完成的服务，编写周报，录制 5–10 分钟 demo（登录->访问 /hello）
- 产出：周报 + demo 步骤文档
- 验收：demo 可运行

—— Week 2：产品服务 + 前端入门（Day 8 ~ Day 14） ——
Day 8（product-service 基本 CRUD）
- 任务：实现 products entity/repo/controller，GET /api/products, GET /api/products/{id}
- 学习：Spring Data JPA 简易用法（1h）
- 产出：product-service CRUD 的基础
- 验收：能在 Postman 中拉到 products

Day 9（前端环境搭建）
- 任务：初始化 frontend（Vite + React + TS）、安装依赖、配置 dev proxy 指向 gateway
- 学习：Vite 与 React 入门教程（2h）
- 产出：前端 skeleton（首页、Products 列表页）
- 验收：npm run dev 能打开页面

Day 10（前端：拉取 product API 并展示）
- 任务：实现 fetch /api/products 并渲染 ProductCard，简单分页
- 学习：React Hooks（useState/useEffect）基础（1.5h）
- 产出：浏览器显示真实产品数据
- 验收：页面能正确展示多个商品

Day 11（product-service 缓存）
- 任务：在 product-service 加 Redis 缓存（Spring Cache / Redis）
- 学习：Spring Cache 与 Redis（0.5h）
- 产出：详情缓存生效
- 验收：多次请求详情时 Redis key 命中

Day 12（前端：产品详情与路由）
- 任务：实现 ProductDetail 页面（React Router 路由）
- 学习：组件拆分与 props 练习（1h）
- 产出：可点击商品进入详情页
- 验收：详情页显示完整信息并能回退

Day 13（前端学习日：状态管理初探）
- 任务：学习并试用 Context / 简单 global state（或开始接触 Redux Toolkit）
- 学习：Redux Toolkit 官方文档（1.5–2h）
- 产出：初步状态管理方案选择
- 验收：前端能保存登录状态并自动携带 token

Day 14（周末整合）
- 任务：修复一周发现���小 bug，编写周报、演示登录->产品列表->详情流程
- 产出：演示脚本、Bug 清单
- 验收：在本地能完成完整用户浏览流程

—— Week 3：购物车、订单与 RabbitMQ（Day 15 ~ Day 21） ——
Day 15（cart-service 设计与实现）
- 任务：设计 Redis 存储（key=cart:{userId}）, 实现 GET/POST/PATCH/DELETE 接口
- 学习：Spring Data Redis 与 JSON 存储（1h）
- 产出：cart-service 基本接口
- 验收：Redis 中可见 cart:{userId} json

Day 16（前端购物车 UI）
- 任务：实现 Add to Cart、Cart 页面、修改 qty、删除 item
- 学习：表单与事件处理（1h）
- 产出：可交互购物车
- 验收：前端操作改变 Redis 中的 cart 值

Day 17（order-service 同步下单）
- 任务：实现 POST /api/orders（读取 cart，校验库存，写 orders+order_items），支持 idempotencyKey
- 学习：事务与幂等策略（0.5h）
- 产出：order-service 能写订单并返回 orderId
- 验收：数据库中有 orders/ order_items 记录

Day 18（RabbitMQ 集成：发布 order.created）
- 任务：集成 Spring AMQP / Spring Cloud Stream，order-service 在下单后 publish order.created
- 学习：Spring AMQP 简要（0.5h）
- 产出：order.created 在 RabbitMQ 管理界面可见
- 验收：消息成功发布到 exchange.queue

Day 19（消息契约与队列设计）
- 任务：编写消息契约文档（order.created, inventory.deduct, inventory.compensate, order.paid）
- 产出：docs/messages.md（包含示例 JSON）
- 验收：成员/自测能理解消息流

Day 20（本地集成演练）
- 任务：端到端演练：登录->加购物车->下单->查看 order.created in RabbitMQ
- 产出：演练记录，traceId 示例
- 验收：可复现的演练脚本在 docs/demo.md

Day 21（周末加速：补缺与学习）
- 任务：若前几天有遗留问题优先修复；学习 Testcontainers 基本用法（1–2h）
- 产出：集成测试 scaffold 开始完成

—— Week 4：库存预扣 & 支付（Day 22 ~ Day 28） ——
Day 22（inventory-listener 设计）
- 任务：设计预扣流程：接收 order.created -> 调用 Redis Lua 脚本原子预扣 -> 根据返回发布 confirm/compensate
- 学习：Lua 脚本基础（1h）
- 产出：设计文档 + Lua 脚本草稿
- 验收：设计文档经 review

Day 23（实现 Redis Lua 原子预扣）
- 任务：编写并在 Java 中调用 Lua（Lettuce），处理成功/失败返回
- 产出：Lua 脚本实际运行（单元测试）
- 验收：在并发模拟下确保没有负库存（初步验证）

Day 24（持久化库存变更与补偿）
- 任务：当预扣成功 -> 异步更新 product.stock（UPDATE ... WHERE stock >= qty），失败 -> publish inventory.compensate
- 学习：乐观锁/UPDATE 条件写法（0.5h）
- 产出：持久化逻辑
- 验收：DB stock 与 Redis 预扣一致（或最终一致）

Day 25（支付服务 skeleton + 回调）
- 任务：实现 payment-service mock 支付（/pay）和回调接口，发布 order.paid
- 学习：支付回调幂等与安全（签名）概念（0.5h）
- 产出：payment 回调能触发 order.paid
- 验收：order-service 接收到 order.paid 并更新订单状态

Day 26（订单状态机与幂等）
- 任务：order-service 订阅 order.paid，更新订单状态为 PAID（并保证幂等）
- 产出：订单状态机实现（CREATED->PAID->SHIPPED）
- 验收：重复回调不改变最终状态

Day 27（并发验证与修复）
- 任务：用简单脚本或 k6 模拟并发下单，观察库存、消息队列与日志
- 产出：并发测试结果与优化点（Lua 脚本/DB索引/队列 prefetch）
- 验收：修复关键问题

Day 28（周末汇总）
- 任务：整合第四周工作、更新 docs（inventory 流程、Lua 脚本、retry 策略）
- 产出：详细 docs/inventory.md
- 验收：docs 更新完成

—— Week 5：Observability、测试、CI 与演示（Day 29 ~ Day 35） ——
Day 29（Micrometer + Prometheus）
- 任务：在服务加入 micrometer-prometheus、配置 /actuator/prometheus 或 metrics endpoint，完善 docker-compose scrape 配置
- 学习：Micrometer 指标定义（0.5h）
- 产出：Prometheus 可采集基本指标
- 验收：Prometheus 页面能看到指标

Day 30（Tracing：OpenTelemetry / Jaeger）
- 任务：集成 OpenTelemetry/Jaeger（或 spring sleuth + jaeger），保证 trace 跨服务传播
- 产出：Jaeger trace 可视化
- 验收：在 Jaeger 上能按 traceId查看完整请求链路

Day 31（集成测试：Testcontainers）
- 任务：编写至少 1 条端到端集成测试（下单 + 预扣 + 模拟支付），用 Testcontainers 启动 MySQL/Redis/RabbitMQ
- 学习：Testcontainers 用法（0.5h）
- 产出：integration test case
- 验收：本地测试通过

Day 32（压测脚本与初次压测）
- 任务：用 k6 编写并发脚本（add-to-cart -> checkout），逐步升压（50->200）
- 产出：k6 脚本 + 压测结果截图/数据
- 验收：记录 error rate & latency；若高于阈值，列出优化项

Day 33（CI 完善：GitHub Actions）
- 任务：完善 .github/workflows/ci.yml（build & test, integration scaffold），确保在 PR 时能运行
- 产出：CI 在 PR 中触发
- 验收：PR 构建通过（如依赖未下载时可先观察日志并修正）

Day 34（Dockerfile / docker-compose.prod 与部署文档）
- 任务：为每个服务补全 Dockerfile（multi-stage），写 docker-compose.prod.yml 示例或简单 k8s manifests
- 产出：Dockerfile 与 prod compose / manifests
- 验收：能本地构建镜像并运行（可选）

Day 35（最终演示、总结与后续计划）
- 任务：准备 10–15 分钟 demo 脚本、录屏（可选），写最终总结（学到的点、未完成项、��续优化）
- 产出：docs/demo.md（演示步骤）、最终总结文档
- 验收：一键启动（docker compose）并演示关键流：登录->浏览->加入购物车->下单->支付->查看订单状态

周末额外建议（若你想更快完成）
- 利用周末做压测、修复和文档工作；也可用来学习更深的前端内容（React 高级、状态管理、TypeScript）或后端的可靠消息/分布式事务主题。

验收标准（最终）
- 功能：注册/登录/浏览商品/购物车/下单/支付（模拟）完整可演示  
- 一致性：并发场景下 product.stock 不 < 0（通过 Redis 预扣 + DB 检查达成）  
- 可观测：Prometheus 能采到基础 metrics，Jaeger 能追踪请求链路  
- CI：PR 能跑 build & tests（integration scaffold）  
- 文档：docs 包含运行步骤、消息契约、演示脚本

常见阻塞与排查建议
- 消息未到达消费者：检查 RabbitMQ 管理界面（queue depth, bindings），查看消费者日志是否报错并导致未 ack。  
- 库存超卖：检查 Lua 脚本逻辑、是否正确返回并在消费端正确处理；DB 更新采用条件更新（WHERE stock >= qty）防止负数。  
- 回调重复：回调署名 + 存 txnId 幂等检查，重复回调直接返回 success 并记录日志。

PR 描述建议（当你把 feature/starter push 并打开 PR 时）
- 标题：feat(starter): add starter mono-repo skeleton (Gradle Kotlin DSL, Java 21) + 5-week plan  
- 内容（示例）：
  - 本 PR 包含：
    - docs/Simplified-Ecommerce-Project-Plan-5weeks.md（压缩版计划）
    - docs/Detailed-5-Week-Daily-Plan.md（逐日计划）
    - 父级 Gradle 文件 & services skeleton（gateway/auth/product/cart/order/inventory/payment）
    - frontend skeleton（Vite + React + TS）
    - infra/docker-compose.dev.yml（MySQL/Redis/RabbitMQ/Jaeger/Prometheus）
    - CI workflow（仅 build & test）
  - 快速启动：
    - docker compose -f infra/docker-compose.dev.yml up -d
    - ./gradlew :product-service:bootRun
    - cd frontend && npm install && npm run dev