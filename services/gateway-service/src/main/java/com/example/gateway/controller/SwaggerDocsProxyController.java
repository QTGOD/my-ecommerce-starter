package com.example.gateway.controller;

import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class SwaggerDocsProxyController {
  private static final Map<String, String> SERVICE_DOC_URLS = Map.of(
      "auth-service", "http://localhost:8081/v3/api-docs",
      "product-service", "http://localhost:8082/v3/api-docs",
      "cart-service", "http://localhost:8083/v3/api-docs",
      "order-service", "http://localhost:8084/v3/api-docs",
      "inventory-listener", "http://localhost:8085/v3/api-docs",
      "payment-service", "http://localhost:8086/v3/api-docs");

  private final RestClient restClient = RestClient.create();

  @GetMapping(value = "/swagger-docs/{serviceName}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> proxyApiDocs(@PathVariable String serviceName) {
    String targetUrl = SERVICE_DOC_URLS.get(serviceName);
    if (targetUrl == null) {
      return ResponseEntity.notFound().build();
    }

    String body = restClient.get()
        .uri(targetUrl)
        .retrieve()
        .body(String.class);

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(body);
  }
}
