import { HeroSection } from "../components/home/HeroSection";
import { LearningPanel } from "../components/home/LearningPanel";
import { ProductSection } from "../components/home/ProductSection";
import { ServiceSection } from "../components/home/ServiceSection";
import { StageSection } from "../components/home/StageSection";
import { products, services, stages } from "../data/catalog";

export function HomePage() {
  return (
    <>
      <HeroSection />

      <main className="content">
        <StageSection stages={stages} />
        <ProductSection products={products} />
        <ServiceSection services={services} />
        <LearningPanel />
      </main>
    </>
  );
}
