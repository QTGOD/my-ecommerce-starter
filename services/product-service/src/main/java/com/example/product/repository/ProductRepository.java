package com.example.product.repository;

import com.example.product.pojo.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
  boolean existsBySlug(String slug);
  boolean existsBySlugAndIdNot(String slug, Long id);
  List<Product> findAllByStatusOrderByIdAsc(String status);
}
