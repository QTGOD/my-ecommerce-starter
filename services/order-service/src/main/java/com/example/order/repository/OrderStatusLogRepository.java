package com.example.order.repository;

import com.example.order.entity.OrderStatusLogEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusLogRepository extends JpaRepository<OrderStatusLogEntity, Long> {
  List<OrderStatusLogEntity> findByOrderIdOrderByIdDesc(Long orderId);
}
