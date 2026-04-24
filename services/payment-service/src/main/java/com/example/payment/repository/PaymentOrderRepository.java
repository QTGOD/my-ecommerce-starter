package com.example.payment.repository;

import com.example.payment.entity.PaymentOrderEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrderEntity, Long> {
  Optional<PaymentOrderEntity> findByOrderId(Long orderId);

  List<PaymentOrderEntity> findByUserIdOrderByIdDesc(Long userId);

  List<PaymentOrderEntity> findByUserIdAndStatusOrderByIdDesc(Long userId, String status);

  List<PaymentOrderEntity> findByStatusOrderByIdDesc(String status);
}
