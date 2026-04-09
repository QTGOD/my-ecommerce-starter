import type { Stage } from "../../types/catalog";

type StageSectionProps = {
  stages: Stage[];
};

export function StageSection({ stages }: StageSectionProps) {
  return (
    <section className="section section-highlight">
      <div className="section-heading">
        <p className="eyebrow">Build Flow</p>
        <h2>把前台浏览、下单链路和运营能力拆成清晰层次</h2>
      </div>

      <div className="stage-grid">
        {stages.map((stage) => (
          <article className="stage-card" key={stage.title}>
            <h3>{stage.title}</h3>
            <p>{stage.summary}</p>
            <ul>
              {stage.items.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </article>
        ))}
      </div>
    </section>
  );
}
