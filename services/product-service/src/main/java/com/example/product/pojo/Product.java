package com.example.product.pojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable = false, length = 128)
  private String name;
  @Column(nullable = false, unique = true, length = 128)
  private String slug;
  @Column(name = "category_id", nullable = false)
  private Long categoryId = 0L;
  @Column(length = 64)
  private String brand;
  @Column(nullable = false, columnDefinition = "TEXT")
  private String description;
  @Column(name = "cover_image", length = 255)
  private String coverImage;
  @Column(nullable = false, length = 32)
  private String status = "ON_SALE";
  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime createdAt;
  @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime updatedAt;

  public Long getId() { return id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getSlug() { return slug; }
  public void setSlug(String slug) { this.slug = slug; }
  public Long getCategoryId() { return categoryId; }
  public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
  public String getBrand() { return brand; }
  public void setBrand(String brand) { this.brand = brand; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public String getCoverImage() { return coverImage; }
  public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
}
