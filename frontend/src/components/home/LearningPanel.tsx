export function LearningPanel() {
  return (
    <section className="section learning-section">
      <div className="section-heading">
        <p className="eyebrow">How To Learn Here</p>
        <h2>这套结构适合边学边改，不容易一下子迷路</h2>
      </div>

      <div className="learning-grid">
        <article className="learning-card">
          <h3>`pages`</h3>
          <p>放页面级组件，一个页面负责组合区块，不要堆业务细节。</p>
        </article>
        <article className="learning-card">
          <h3>`components`</h3>
          <p>放可复用 UI 区块，后续你加购物车、导航栏、表单都放这里。</p>
        </article>
        <article className="learning-card">
          <h3>`data` 与 `services`</h3>
          <p>前者适合 mock 数据，后者适合真实请求。学习时替换起来最直观。</p>
        </article>
      </div>
    </section>
  );
}
