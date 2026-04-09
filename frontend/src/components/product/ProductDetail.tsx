import { Link } from "react-router-dom";
import type { Product } from "../../types/catalog";

type ProductDetailProps = {
  product: Product;
};

export function ProductDetail({ product }: ProductDetailProps) {
  return (
    <div className="product-detail-page">
      <section className="section breadcrumb-section">
        <Link className="back-link" to="/products">
          返回商品列表
        </Link>
      </section>

      <section className="section detail-layout">
        <div className="detail-visual" />

        <div className="detail-copy">
          <p className="eyebrow">{product.category}</p>
          <h1 className="page-title">{product.name}</h1>
          <p className="product-description">{product.description}</p>

          <div className="detail-price-row">
            <span className="product-price">{product.price}</span>
            <span className="product-badge">{product.badge}</span>
          </div>

          <div className="detail-actions">
            <button className="primary-button" type="button">
              加入购物车
            </button>
            <button className="ghost-button" type="button">
              收藏
            </button>
          </div>
        </div>
      </section>

      <section className="section">
        <div className="section-heading">
          <p className="eyebrow">Highlights</p>
          <h2>商品详情页最适合练习字段展示和布局拆分</h2>
        </div>

        <div className="highlight-grid">
          {product.highlights.map((highlight) => (
            <article className="learning-card" key={highlight}>
              <h3>{highlight}</h3>
              <p>后面你可以把这里扩展成规格参数、评价摘要或售后说明。</p>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}
