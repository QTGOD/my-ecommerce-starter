package com.example.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_reservations")
public class InventoryReservationEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "order_id", nullable = false, unique = true)
  private Long orderId;
  @Column(name = "user_id", nullable = false)
  private Long userId;
  @Column(nullable = false, length = 32)
  private String status = "RESERVED";
  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime createdAt;
  @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime updatedAt;

  public Long getId() { return id; }
  public Long getOrderId() { return orderId; }
  public void setOrderId(Long orderId) { this.orderId = orderId; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
