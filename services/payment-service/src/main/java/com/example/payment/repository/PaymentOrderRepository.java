package com.example.payment.repository;

import java.util.Optional;

import com.example.payment.entity.PaymentOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrderEntity, Long> {
  Optional<PaymentOrderEntity> findByOrderId(Long orderId);
}
