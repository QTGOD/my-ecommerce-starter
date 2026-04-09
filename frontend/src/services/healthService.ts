import { apiClient } from "./apiClient";

export async function fetchGatewayHello() {
  const response = await apiClient.get<string>("/hello");
  return response.data;
}
