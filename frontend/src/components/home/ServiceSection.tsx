import type { ServiceCard, ServiceStatus } from "../../types/catalog";

type ServiceSectionProps = {
  services: ServiceCard[];
};

const statusText: Record<ServiceStatus, string> = {
  healthy: "Ready",
  starting: "Wiring",
  planned: "Planned",
};

export function ServiceSection({ services }: ServiceSectionProps) {
  return (
    <section className="section" id="services">
      <div className="section-heading">
        <p className="eyebrow">Service Readiness</p>
        <h2>把后端模块的接入状态直接放到页面里，联调时更直观</h2>
      </div>

      <div className="service-list">
        {services.map((service) => (
          <article className="service-card" key={service.name}>
            <div>
              <div className="service-title-row">
                <h3>{service.name}</h3>
                <span className={`status-pill status-${service.status}`}>
                  {statusText[service.status]}
                </span>
              </div>
              <p>{service.description}</p>
            </div>
            <code>{service.endpoint}</code>
          </article>
        ))}
      </div>
    </section>
  );
}
