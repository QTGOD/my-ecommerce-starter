package com.example.inventory.repository;

import java.util.List;
import java.util.Optional;

import com.example.inventory.entity.InventoryReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservationEntity, Long> {
  Optional<InventoryReservationEntity> findByOrderId(Long orderId);
  List<InventoryReservationEntity> findAllByOrderByIdDesc();
  List<InventoryReservationEntity> findAllByStatusOrderByIdDesc(String status);
  List<InventoryReservationEntity> findAllByUserIdOrderByIdDesc(Long userId);
  List<InventoryReservationEntity> findAllByUserIdAndStatusOrderByIdDesc(Long userId, String status);
}
