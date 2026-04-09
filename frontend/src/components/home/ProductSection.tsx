import { Link } from "react-router-dom";
import type { Product } from "../../types/catalog";

type ProductSectionProps = {
  products: Product[];
};

export function ProductSection({ products }: ProductSectionProps) {
  return (
    <section className="section" id="catalog">
      <div className="section-heading">
        <p className="eyebrow">Catalog Mock</p>
        <h2>先用静态卡片占位，后面接商品接口时可以平滑替换</h2>
      </div>

      <div className="product-grid">
        {products.map((product) => (
          <article className="product-card" key={product.name}>
            <span className="product-badge">{product.badge}</span>
            <div className="product-visual" />
            <div className="product-meta">
              <p className="product-category">{product.category}</p>
              <h3>{product.name}</h3>
              <p className="product-description">{product.description}</p>
              <div className="product-row">
                <span className="product-price">{product.price}</span>
                <Link className="product-link" to={`/products/${product.slug}`}>
                  查看详情
                </Link>
              </div>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
