package com.example.product.repository;

import java.util.List;

import com.example.product.pojo.ProductSku;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductSkuRepository extends JpaRepository<ProductSku, Long> {
  List<ProductSku> findByProductIdOrderByIdAsc(Long productId);
  boolean existsBySkuCode(String skuCode);
}
