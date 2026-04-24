package com.example.inventory.service;

import com.example.common.exception.BusinessException;
import com.example.common.exception.ErrorCode;
import com.example.inventory.entity.ConsumedMessageEntity;
import com.example.inventory.entity.InventoryReservationEntity;
import com.example.inventory.repository.ConsumedMessageRepository;
import com.example.inventory.repository.InventoryReservationRepository;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
  private static final Set<String> RESERVATION_STATUSES =
      Set.of("RESERVED", "CONFIRMED", "RELEASED", "CANCELLED");

  private final InventoryReservationRepository inventoryReservationRepository;
  private final ConsumedMessageRepository consumedMessageRepository;

  public InventoryService(
      InventoryReservationRepository inventoryReservationRepository,
      ConsumedMessageRepository consumedMessageRepository) {
    this.inventoryReservationRepository = inventoryReservationRepository;
    this.consumedMessageRepository = consumedMessageRepository;
  }

  @Transactional
  public InventoryReservationEntity upsertReservation(Long orderId, Long userId, String status) {
    InventoryReservationEntity reservation = inventoryReservationRepository.findByOrderId(orderId)
        .orElseGet(InventoryReservationEntity::new);
    reservation.setOrderId(orderId);
    reservation.setUserId(userId);
    reservation.setStatus(normalizeStatus(status));
    return inventoryReservationRepository.save(reservation);
  }

  @Transactional(readOnly = true)
  public InventoryReservationEntity getReservation(Long orderId) {
    return inventoryReservationRepository.findByOrderId(orderId)
        .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_RESERVATION_NOT_FOUND));
  }

  @Transactional(readOnly = true)
  public List<InventoryReservationEntity> listReservations(Long userId, String status) {
    String normalizedStatus = status == null || status.isBlank() ? null : normalizeStatus(status);
    if (userId != null && normalizedStatus != null) {
      return inventoryReservationRepository.findAllByUserIdAndStatusOrderByIdDesc(userId, normalizedStatus);
    }
    if (userId != null) {
      return inventoryReservationRepository.findAllByUserIdOrderByIdDesc(userId);
    }
    if (normalizedStatus != null) {
      return inventoryReservationRepository.findAllByStatusOrderByIdDesc(normalizedStatus);
    }
    return inventoryReservationRepository.findAllByOrderByIdDesc();
  }

  @Transactional
  public InventoryReservationEntity updateReservationStatus(Long orderId, String status) {
    InventoryReservationEntity reservation = getReservation(orderId);
    reservation.setStatus(normalizeStatus(status));
    return inventoryReservationRepository.save(reservation);
  }

  @Transactional
  public boolean markConsumed(String messageId, String consumerName, String eventType) {
    boolean duplicate = consumedMessageRepository.existsByMessageIdAndConsumerName(messageId, consumerName);
    if (!duplicate) {
      ConsumedMessageEntity message = new ConsumedMessageEntity();
      message.setMessageId(messageId);
      message.setConsumerName(consumerName);
      message.setEventType(eventType);
      consumedMessageRepository.save(message);
    }
    return duplicate;
  }

  private String normalizeStatus(String status) {
    if (status == null || status.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST, "Unsupported reservation status");
    }
    String normalizedStatus = status.trim().toUpperCase();
    if (!RESERVATION_STATUSES.contains(normalizedStatus)) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST, "Unsupported reservation status");
    }
    return normalizedStatus;
  }
}
