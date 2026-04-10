package com.example.order.repository;

import java.util.List;

import com.example.order.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {
  List<OrderItemEntity> findByOrderId(Long orderId);
}
