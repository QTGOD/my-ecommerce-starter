import { Link } from "react-router-dom";
import type { Product } from "../../types/catalog";

type ProductListProps = {
  products: Product[];
};

export function ProductList({ products }: ProductListProps) {
  return (
    <div className="product-list-page">
      <section className="section page-hero">
        <div className="section-heading">
          <p className="eyebrow">Product Catalog</p>
          <h1 className="page-title">商品列表示例</h1>
          <p className="page-description">
            这里适合继续练习筛选、搜索、分页和“加入购物车”等常见电商功能。
          </p>
        </div>
      </section>

      <section className="section">
        <div className="product-grid">
          {products.map((product) => (
            <article className="product-card" key={product.slug}>
              <span className="product-badge">{product.badge}</span>
              <div className="product-visual" />
              <div className="product-meta">
                <p className="product-category">{product.category}</p>
                <h3>{product.name}</h3>
                <p className="product-description">{product.description}</p>
                <div className="product-row">
                  <span className="product-price">{product.price}</span>
                  <Link className="product-link" to={`/products/${product.slug}`}>
                    进入详情
                  </Link>
                </div>
              </div>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}
