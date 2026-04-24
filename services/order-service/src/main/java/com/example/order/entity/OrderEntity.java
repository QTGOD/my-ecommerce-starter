package com.example.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class OrderEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "order_no", nullable = false, unique = true, length = 64)
  private String orderNo;
  @Column(name = "user_id", nullable = false)
  private Long userId;
  @Column(nullable = false, length = 32)
  private String status = "CREATED";
  @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
  private BigDecimal totalAmount;
  @Column(name = "payable_amount", nullable = false, precision = 10, scale = 2)
  private BigDecimal payableAmount;
  @Column(name = "payment_status", nullable = false, length = 32)
  private String paymentStatus = "INIT";
  @Column(name = "delivery_status", nullable = false, length = 32)
  private String deliveryStatus = "PENDING";
  @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
  private String idempotencyKey;
  @Column(length = 255)
  private String remark;
  @Column(name = "receiver_name", nullable = false, length = 64)
  private String receiverName;
  @Column(name = "receiver_phone", nullable = false, length = 32)
  private String receiverPhone;
  @Column(name = "receiver_address_snapshot", nullable = false, length = 255)
  private String receiverAddressSnapshot;
  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime createdAt;
  @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime updatedAt;

  public Long getId() { return id; }
  public String getOrderNo() { return orderNo; }
  public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public BigDecimal getTotalAmount() { return totalAmount; }
  public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
  public BigDecimal getPayableAmount() { return payableAmount; }
  public void setPayableAmount(BigDecimal payableAmount) { this.payableAmount = payableAmount; }
  public String getPaymentStatus() { return paymentStatus; }
  public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
  public String getDeliveryStatus() { return deliveryStatus; }
  public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }
  public String getIdempotencyKey() { return idempotencyKey; }
  public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
  public String getReceiverName() { return receiverName; }
  public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
  public String getReceiverPhone() { return receiverPhone; }
  public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
  public String getReceiverAddressSnapshot() { return receiverAddressSnapshot; }
  public void setReceiverAddressSnapshot(String receiverAddressSnapshot) { this.receiverAddressSnapshot = receiverAddressSnapshot; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
}
