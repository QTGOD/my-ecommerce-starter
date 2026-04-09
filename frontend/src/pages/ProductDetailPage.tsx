import { Navigate, useParams } from "react-router-dom";
import { ProductDetail } from "../components/product/ProductDetail";
import { getProductBySlug } from "../services/catalogService";

export function ProductDetailPage() {
  const { slug } = useParams();

  if (!slug) {
    return <Navigate to="/products" replace />;
  }

  const product = getProductBySlug(slug);

  if (!product) {
    return <Navigate to="/products" replace />;
  }

  return (
    <main className="content">
      <ProductDetail product={product} />
    </main>
  );
}
