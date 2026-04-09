export function HeroSection() {
  return (
    <header className="hero">
      <div className="hero-copy">
        <p className="eyebrow">Starter Frontend</p>
        <h1>为电商微服务准备一个真正能继续演进的前端骨架</h1>
        <p className="hero-text">
          这个壳子先把首页、业务模块、服务状态和后续接入点铺好。你接下来无论是联调
          Spring Boot 接口，还是补商品流、订单流，都可以直接往现有结构里填。
        </p>

        <div className="hero-actions">
          <a className="primary-action" href="#catalog">
            查看商品区块
          </a>
          <a className="secondary-action" href="#services">
            查看服务状态
          </a>
        </div>
      </div>

      <aside className="hero-panel">
        <div className="panel-label">Launch Checklist</div>
        <div className="metric-grid">
          <div className="metric-card">
            <span className="metric-value">7</span>
            <span className="metric-label">Backend Services</span>
          </div>
          <div className="metric-card">
            <span className="metric-value">3</span>
            <span className="metric-label">Frontend Zones</span>
          </div>
          <div className="metric-card">
            <span className="metric-value">21</span>
            <span className="metric-label">Java Toolchain</span>
          </div>
          <div className="metric-card">
            <span className="metric-value">Vite</span>
            <span className="metric-label">Fast Iteration</span>
          </div>
        </div>
      </aside>
    </header>
  );
}
