import type { Product, ServiceCard, Stage } from "../types/catalog";

export const services: ServiceCard[] = [
  {
    name: "Gateway Service",
    description: "聚合入口、路由转发与统一鉴权的首个接入点。",
    endpoint: "GET /hello",
    status: "healthy",
  },
  {
    name: "Auth Service",
    description: "负责用户身份、会话与后续权限扩展。",
    endpoint: "GET /hello",
    status: "healthy",
  },
  {
    name: "Product Service",
    description: "承载商品目录、详情与搜索扩展能力。",
    endpoint: "GET /hello",
    status: "starting",
  },
  {
    name: "Order + Payment",
    description: "下单、库存事件与支付模拟的业务闭环。",
    endpoint: "GET /hello",
    status: "planned",
  },
];

export const stages: Stage[] = [
  {
    title: "Discover",
    summary: "从首页活动、品类橱窗到商品卡片，先把用户带入购买场景。",
    items: ["活动 Banner", "品类导航", "搜索与筛选"],
  },
  {
    title: "Convert",
    summary: "把购物车、库存与支付流程拆成可以逐步落地的子能力。",
    items: ["购物车状态", "库存校验", "订单确认"],
  },
  {
    title: "Operate",
    summary: "为后续接入监控、推荐和运营配置预留足够位置。",
    items: ["服务健康页", "实验位", "运营指标"],
  },
];

export const products: Product[] = [
  {
    slug: "nebula-headset",
    name: "Nebula Headset",
    price: "$149",
    category: "Audio",
    badge: "Best Seller",
    description: "为长时间游戏和日常通勤准备的轻量头戴耳机，强调舒适与空间感。",
    highlights: ["50mm 驱动单元", "双设备切换", "轻量化头梁"],
  },
  {
    slug: "pulse-keyboard",
    name: "Pulse Keyboard",
    price: "$119",
    category: "Peripherals",
    badge: "Low Latency",
    description: "面向游戏和编码场景的紧凑型机械键盘，适合做首批电商详情页练习。",
    highlights: ["热插拔轴体", "75% 配列", "RGB 灯效"],
  },
  {
    slug: "atlas-monitor",
    name: "Atlas Monitor",
    price: "$399",
    category: "Display",
    badge: "144Hz",
    description: "兼顾办公和电竞的高刷显示器，适合演示不同规格字段如何落到页面上。",
    highlights: ["27 英寸", "2K 分辨率", "144Hz 刷新率"],
  },
  {
    slug: "nova-mouse",
    name: "Nova Mouse",
    price: "$69",
    category: "Accessories",
    badge: "Ergonomic",
    description: "注重握持手感与自定义按键的无线鼠标，适合练习产品卡片和详情页联动。",
    highlights: ["双模连接", "可编程按键", "人体工学外形"],
  },
];
