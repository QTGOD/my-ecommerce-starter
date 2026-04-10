package com.example.order.repository;

import java.util.List;
import java.util.Optional;

import com.example.order.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
  Optional<OrderEntity> findByIdempotencyKey(String idempotencyKey);
  List<OrderEntity> findByUserIdOrderByIdDesc(Long userId);
}
