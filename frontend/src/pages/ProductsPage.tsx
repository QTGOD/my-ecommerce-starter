import { ProductList } from "../components/product/ProductList";
import { getAllProducts } from "../services/catalogService";

export function ProductsPage() {
  const products = getAllProducts();

  return (
    <main className="content">
      <ProductList products={products} />
    </main>
  );
}
