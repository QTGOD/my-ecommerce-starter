package com.example.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItemEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "order_id", nullable = false)
  private Long orderId;
  @Column(name = "product_id", nullable = false)
  private Long productId;
  @Column(name = "sku_id", nullable = false)
  private Long skuId;
  @Column(name = "product_name_snapshot", nullable = false, length = 128)
  private String productNameSnapshot;
  @Column(name = "sku_desc_snapshot", length = 255)
  private String skuDescSnapshot;
  @Column(name = "cover_image_snapshot", length = 255)
  private String coverImageSnapshot;
  @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
  private BigDecimal unitPrice;
  @Column(nullable = false)
  private Integer quantity;
  @Column(name = "line_total", nullable = false, precision = 10, scale = 2)
  private BigDecimal lineTotal;

  public Long getId() { return id; }
  public Long getOrderId() { return orderId; }
  public void setOrderId(Long orderId) { this.orderId = orderId; }
  public Long getProductId() { return productId; }
  public void setProductId(Long productId) { this.productId = productId; }
  public Long getSkuId() { return skuId; }
  public void setSkuId(Long skuId) { this.skuId = skuId; }
  public String getProductNameSnapshot() { return productNameSnapshot; }
  public void setProductNameSnapshot(String productNameSnapshot) { this.productNameSnapshot = productNameSnapshot; }
  public String getSkuDescSnapshot() { return skuDescSnapshot; }
  public void setSkuDescSnapshot(String skuDescSnapshot) { this.skuDescSnapshot = skuDescSnapshot; }
  public String getCoverImageSnapshot() { return coverImageSnapshot; }
  public void setCoverImageSnapshot(String coverImageSnapshot) { this.coverImageSnapshot = coverImageSnapshot; }
  public BigDecimal getUnitPrice() { return unitPrice; }
  public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
  public Integer getQuantity() { return quantity; }
  public void setQuantity(Integer quantity) { this.quantity = quantity; }
  public BigDecimal getLineTotal() { return lineTotal; }
  public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
}
