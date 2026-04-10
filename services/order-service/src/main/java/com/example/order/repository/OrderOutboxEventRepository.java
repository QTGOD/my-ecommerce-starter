package com.example.order.repository;

import com.example.order.entity.dto.OrderOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderOutboxEventRepository extends JpaRepository<OrderOutboxEvent, Long> {}
