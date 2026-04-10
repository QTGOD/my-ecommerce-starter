package com.example.inventory.repository;

import java.util.Optional;

import com.example.inventory.entity.InventoryReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservationEntity, Long> {
  Optional<InventoryReservationEntity> findByOrderId(Long orderId);
}
