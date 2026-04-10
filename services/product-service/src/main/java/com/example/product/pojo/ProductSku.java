package com.example.product.pojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "product_skus")
public class ProductSku {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "product_id", nullable = false)
  private Long productId;
  @Column(name = "sku_code", nullable = false, unique = true, length = 64)
  private String skuCode;
  @Column(name = "spec_json", nullable = false, columnDefinition = "json")
  private String specJson;
  @Column(name = "sale_price", nullable = false, precision = 10, scale = 2)
  private BigDecimal salePrice;
  @Column(name = "market_price", precision = 10, scale = 2)
  private BigDecimal marketPrice;
  @Column(nullable = false)
  private Integer stock = 0;
  @Column(name = "locked_stock", nullable = false)
  private Integer lockedStock = 0;
  @Column(nullable = false, length = 32)
  private String status = "ON_SALE";

  public Long getId() { return id; }
  public Long getProductId() { return productId; }
  public void setProductId(Long productId) { this.productId = productId; }
  public String getSkuCode() { return skuCode; }
  public void setSkuCode(String skuCode) { this.skuCode = skuCode; }
  public String getSpecJson() { return specJson; }
  public void setSpecJson(String specJson) { this.specJson = specJson; }
  public BigDecimal getSalePrice() { return salePrice; }
  public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
  public BigDecimal getMarketPrice() { return marketPrice; }
  public void setMarketPrice(BigDecimal marketPrice) { this.marketPrice = marketPrice; }
  public Integer getStock() { return stock; }
  public void setStock(Integer stock) { this.stock = stock; }
  public Integer getLockedStock() { return lockedStock; }
  public void setLockedStock(Integer lockedStock) { this.lockedStock = lockedStock; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
