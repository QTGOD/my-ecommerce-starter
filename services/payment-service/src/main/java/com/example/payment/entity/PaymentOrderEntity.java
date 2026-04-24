package com.example.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_orders")
public class PaymentOrderEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "payment_no", nullable = false, unique = true, length = 64)
  private String paymentNo;
  @Column(name = "order_id", nullable = false)
  private Long orderId;
  @Column(name = "user_id", nullable = false)
  private Long userId;
  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal amount;
  @Column(name = "payment_method", nullable = false, length = 32)
  private String paymentMethod;
  @Column(nullable = false, length = 32)
  private String status = "INIT";
  @Column(name = "third_party_txn_id", unique = true, length = 64)
  private String thirdPartyTxnId;
  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime createdAt;
  @Column(name = "paid_at")
  private LocalDateTime paidAt;
  @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime updatedAt;

  public Long getId() { return id; }
  public String getPaymentNo() { return paymentNo; }
  public void setPaymentNo(String paymentNo) { this.paymentNo = paymentNo; }
  public Long getOrderId() { return orderId; }
  public void setOrderId(Long orderId) { this.orderId = orderId; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  public String getPaymentMethod() { return paymentMethod; }
  public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getThirdPartyTxnId() { return thirdPartyTxnId; }
  public void setThirdPartyTxnId(String thirdPartyTxnId) { this.thirdPartyTxnId = thirdPartyTxnId; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public LocalDateTime getPaidAt() { return paidAt; }
  public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
}
