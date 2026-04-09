export type ServiceStatus = "healthy" | "starting" | "planned";

export type ServiceCard = {
  name: string;
  description: string;
  endpoint: string;
  status: ServiceStatus;
};

export type Stage = {
  title: string;
  summary: string;
  items: string[];
};

export type Product = {
  slug: string;
  name: string;
  price: string;
  category: string;
  badge: string;
  description: string;
  highlights: string[];
};
