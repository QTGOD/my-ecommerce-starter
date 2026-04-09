#!/usr/bin/env bash
set -euo pipefail

ROOT="$(pwd)"
BRANCH="$(git rev-parse --abbrev-ref HEAD || echo feature/starter)"
echo "Working in repo root: $ROOT on branch: $BRANCH"

confirm_overwrite() {
  local f="$1"
  if [ -e "$f" ]; then
    echo "File $f already exists. Skipping creation. (To overwrite, remove it manually first)"
    return 1
  fi
  return 0
}

mkdir -p docs services libs common infra frontend .github/workflows

# 1) docs: 两份 md（5 周版）
DOC1="docs/Simplified-Ecommerce-Project-Plan-5weeks.md"
if confirm_overwrite "$DOC1"; then
cat > "$DOC1" <<'MD'
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
MD
else
  echo "Skipped $DOC1"
fi

DOC2="docs/Detailed-5-Week-Daily-Plan.md"
if confirm_overwrite "$DOC2"; then
cat > "$DOC2" <<'MD'
# 逐日 5 周执行计划（每天建议工作 8 小时）
版本：2026-04-09

说明：把原 8 周拆成紧凑 5 周安排（含周末可选冲刺），每日列出核心任务、学习点与交付物，适合时间充裕且想加快迭代的个人。

（正文略——仓库里保留更详细的每日分配，或在需要时由助理展开每日日程）
MD
else
  echo "Skipped $DOC2"
fi

# 2) root README.md
README="README.md"
if confirm_overwrite "$README"; then
cat > "$README" <<'MD'
# my-ecommerce-starter

Starter mono-repo for Simplified E-commerce Project
Tech: Java 21, Spring Boot 3.x, Gradle (Kotlin DSL), RabbitMQ, Redis, MySQL, React (Vite)

Quick start:
1. Start infra:
   docker compose -f infra/docker-compose.dev.yml up -d
2. Build or run services:
   ./gradlew :product-service:bootRun
3. Frontend:
   cd frontend
   npm install
   npm run dev

For full instructions see docs/
MD
else
  echo "Skipped $README"
fi

# 3) Gradle parent files (Kotlin DSL)
SETTINGS="settings.gradle.kts"
if confirm_overwrite "$SETTINGS"; then
cat > "$SETTINGS" <<'KT'
rootProject.name = "my-ecommerce-starter"

include(
    "libs:common",
    "services:gateway-service",
    "services:auth-service",
    "services:product-service",
    "services:cart-service",
    "services:order-service",
    "services:inventory-listener",
    "services:payment-service"
)
KT
else
  echo "Skipped $SETTINGS"
fi

BUILD="build.gradle.kts"
if confirm_overwrite "$BUILD"; then
cat > "$BUILD" <<'KT'
plugins {
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

allprojects {
    repositories {
        mavenCentral()
    }
}
KT
else
  echo "Skipped $BUILD"
fi

# Helper to create minimal service module
create_service() {
  local svc_dir="$1"
  local pkg="$2" # e.g. com.example.auth
  mkdir -p "$svc_dir/src/main/java/$(echo $pkg | tr . /)"
  mkdir -p "$svc_dir/src/main/resources"
  mkdir -p "$svc_dir/src/test/java"
  # build.gradle.kts for module
  local buildf="$svc_dir/build.gradle.kts"
  if confirm_overwrite "$buildf"; then
cat > "$buildf" <<KT
plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.0"
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
KT
  else
    echo "Skipped $buildf"
  fi

  # Application.java
  local appf="$svc_dir/src/main/java/$(echo $pkg | tr . /)/Application.java"
  if confirm_overwrite "$appf"; then
cat > "$appf" <<JAVA
package $pkg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
JAVA
  else
    echo "Skipped $appf"
  fi

  # sample controller
  local ctrlf="$svc_dir/src/main/java/$(echo $pkg | tr . /)/HelloController.java"
  if confirm_overwrite "$ctrlf"; then
cat > "$ctrlf" <<JAVA
package $pkg;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
  @GetMapping("/hello")
  public String hello() {
    return "Hello from $pkg";
  }
}
JAVA
  else
    echo "Skipped $ctrlf"
  fi

  # Dockerfile
  local dockerf="$svc_dir/Dockerfile"
  if confirm_overwrite "$dockerf"; then
cat > "$dockerf" <<DOCK
# multi-stage build
FROM gradle:8.6-jdk21 AS build
WORKDIR /home/gradle/project
COPY . .
RUN gradle bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /home/gradle/project/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
DOCK
  else
    echo "Skipped $dockerf"
  fi
}

# 4) libs/common module
COMMON_DIR="libs/common"
mkdir -p "$COMMON_DIR/src/main/java/com/example/common"
COMMON_BUILD="$COMMON_DIR/build.gradle.kts"
if confirm_overwrite "$COMMON_BUILD"; then
cat > "$COMMON_BUILD" <<KT
plugins {
    java
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
KT
fi
COMMON_PKG="$COMMON_DIR/src/main/java/com/example/common/Constants.java"
if confirm_overwrite "$COMMON_PKG"; then
cat > "$COMMON_PKG" <<JAVA
package com.example.common;

public class Constants {
    public static final String VERSION = "0.1.0";
}
JAVA
fi

# 5) services: create minimal modules
create_service "services/gateway-service" "com.example.gateway"
create_service "services/auth-service" "com.example.auth"
create_service "services/product-service" "com.example.product"
create_service "services/cart-service" "com.example.cart"
create_service "services/order-service" "com.example.order"
create_service "services/inventory-listener" "com.example.inventory"
create_service "services/payment-service" "com.example.payment"

# 6) infra/docker-compose.dev.yml
DOCKER_COMPOSE="infra/docker-compose.dev.yml"
if confirm_overwrite "$DOCKER_COMPOSE"; then
cat > "$DOCKER_COMPOSE" <<YAML
version: '3.8'
services:
  mysql:
    image: mysql:8
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: shop
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
  redis:
    image: redis:7
    ports:
      - "6379:6379"
  rabbitmq:
    image: rabbitmq:3-management
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    ports:
      - "5672:5672"
      - "15672:15672"
  jaeger:
    image: jaegertracing/all-in-one:1.41
    ports:
      - "16686:16686"
  prometheus:
    image: prom/prometheus
    ports:
      - "9090:9090"
volumes:
  mysql_data:
YAML
fi

# 7) frontend skeleton (Vite + React + TS)
FRONT_PKG="frontend/package.json"
if confirm_overwrite "$FRONT_PKG"; then
mkdir -p frontend/src
cat > "$FRONT_PKG" <<JSON
{
  "name": "frontend",
  "version": "0.1.0",
  "private": true,
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "axios": "^1.4.0",
    "react-router-dom": "^6.14.1"
  },
  "devDependencies": {
    "typescript": "^5.2.2",
    "vite": "^5.0.0",
    "@types/react": "^18.2.25",
    "@types/react-dom": "^18.2.7"
  }
}
JSON
fi

FRONT_MAIN="frontend/src/main.tsx"
if confirm_overwrite "$FRONT_MAIN"; then
cat > "$FRONT_MAIN" <<TS
import React from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'

createRoot(document.getElementById('root')!).render(<App />)
TS
fi

FRONT_APP="frontend/src/App.tsx"
if confirm_overwrite "$FRONT_APP"; then
cat > "$FRONT_APP" <<TSX
import React from 'react'

export default function App() {
  return (
    <div style={{padding:20}}>
      <h2>My Ecommerce Starter (frontend)</h2>
      <p>Open /hello endpoints of backend services for quick checks.</p>
    </div>
  )
}
TSX
fi

FRONT_INDEX="frontend/index.html"
if confirm_overwrite "$FRONT_INDEX"; then
cat > "$FRONT_INDEX" <<HTML
<!doctype html>
<html>
  <head>
    <meta charset="utf-8" />
    <title>Frontend</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
HTML
fi

# 8) GitHub Actions (CI: build & test only)
CI_FILE=".github/workflows/ci.yml"
if confirm_overwrite "$CI_FILE"; then
cat > "$CI_FILE" <<YML
name: CI

on:
  push:
    branches: [ main, feature/starter ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
      - name: Build with Gradle
        run: ./gradlew build -x test --no-daemon
      - name: Run unit tests
        run: ./gradlew test --no-daemon
YML
fi

# 9) Testcontainers scaffold (basic)
TC_DIR="services/integration-test"
if [ ! -d "$TC_DIR" ]; then
mkdir -p "$TC_DIR/src/test/java"
cat > "$TC_DIR/build.gradle.kts" <<KT
plugins {
    java
}

dependencies {
    testImplementation("org.testcontainers:testcontainers:1.19.0")
    testImplementation("org.testcontainers:junit-jupiter:1.19.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
}
KT

cat > "$TC_DIR/src/test/java/PlaceholderIT.java" <<JAVA
// Placeholder for integration tests using Testcontainers
public class PlaceholderIT {
    // implement tests as you progress
}
JAVA
fi

# 10) top-level .gitignore
GITIGNORE=".gitignore"
if confirm_overwrite "$GITIGNORE"; then
cat > "$GITIGNORE" <<TXT
/.gradle
/build
/.idea
/.vscode
/node_modules
/frontend/dist
*.log
TXT
fi

# Final git add & commit
echo "Adding files to git..."
git add -A

if git diff --staged --quiet; then
  echo "No staged changes to commit."
else
  git commit -m "feat(starter): add starter mono-repo skeleton and 5-week plan"
  echo "Committed changes. Please review, then push with:"
  echo "  git push -u origin $BRANCH"
fi

echo "Done. Files created. Note: script does not push changes to remote."