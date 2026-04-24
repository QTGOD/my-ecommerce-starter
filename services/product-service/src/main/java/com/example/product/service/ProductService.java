package com.example.product.service;

import com.example.common.exception.BusinessException;
import com.example.common.exception.ErrorCode;
import com.example.product.pojo.Product;
import com.example.product.pojo.ProductSku;
import com.example.product.repository.ProductRepository;
import com.example.product.repository.ProductSkuRepository;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
  private static final Set<String> PRODUCT_STATUSES = Set.of("DRAFT", "ON_SALE", "OFF_SHELF");
  private static final Set<String> SKU_STATUSES = Set.of("ON_SALE", "OFF_SHELF");

  private final ProductRepository productRepository;
  private final ProductSkuRepository productSkuRepository;

  public ProductService(ProductRepository productRepository, ProductSkuRepository productSkuRepository) {
    this.productRepository = productRepository;
    this.productSkuRepository = productSkuRepository;
  }

  @Transactional(readOnly = true)
  public List<Product> listProducts(String status) {
    if (status == null || status.isBlank()) {
      return productRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }
    String normalizedStatus = normalizeStatus(status, PRODUCT_STATUSES, "Unsupported product status");
    return productRepository.findAllByStatusOrderByIdAsc(normalizedStatus);
  }

  @Transactional(readOnly = true)
  public Product getProduct(Long id) {
    return productRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
  }

  @Transactional(readOnly = true)
  public List<ProductSku> getProductSkus(Long productId) {
    return productSkuRepository.findByProductIdOrderByIdAsc(productId);
  }

  @Transactional
  public Product createProduct(Product product, ProductSku sku) {
    validateSlugUnique(product.getSlug(), null);
    validateSkuCodeUnique(sku.getSkuCode());
    validateStock(sku.getStock(), sku.getLockedStock());
    productRepository.save(product);
    sku.setProductId(product.getId());
    productSkuRepository.save(sku);
    return product;
  }

  @Transactional
  public Product updateProduct(Long id, Product updates) {
    Product product = getProduct(id);
    validateSlugUnique(updates.getSlug(), id);
    product.setName(updates.getName());
    product.setSlug(updates.getSlug());
    product.setCategoryId(updates.getCategoryId());
    product.setBrand(updates.getBrand());
    product.setDescription(updates.getDescription());
    product.setCoverImage(updates.getCoverImage());
    if (updates.getStatus() != null) {
      product.setStatus(normalizeStatus(updates.getStatus(), PRODUCT_STATUSES, "Unsupported product status"));
    }
    return productRepository.save(product);
  }

  @Transactional
  public Product updateProductStatus(Long id, String status) {
    Product product = getProduct(id);
    product.setStatus(normalizeStatus(status, PRODUCT_STATUSES, "Unsupported product status"));
    return productRepository.save(product);
  }

  @Transactional
  public ProductSku addSku(Long productId, ProductSku sku) {
    getProduct(productId);
    validateSkuCodeUnique(sku.getSkuCode());
    sku.setProductId(productId);
    sku.setStatus(normalizeStatus(sku.getStatus(), SKU_STATUSES, "Unsupported sku status"));
    validateStock(sku.getStock(), sku.getLockedStock());
    return productSkuRepository.save(sku);
  }

  @Transactional
  public ProductSku updateSkuInventory(Long productId, Long skuId, Integer stock, Integer stockDelta, String status) {
    getProduct(productId);
    ProductSku sku = productSkuRepository.findById(skuId)
        .filter(item -> item.getProductId().equals(productId))
        .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "SKU not found"));

    if (stock != null && stockDelta != null) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST, "Provide either stock or stockDelta, not both");
    }

    int nextStock = sku.getStock();
    if (stock != null) {
      nextStock = stock;
    } else if (stockDelta != null) {
      nextStock = sku.getStock() + stockDelta;
    }

    validateStock(nextStock, sku.getLockedStock());
    sku.setStock(nextStock);

    if (status != null && !status.isBlank()) {
      sku.setStatus(normalizeStatus(status, SKU_STATUSES, "Unsupported sku status"));
    }
    return productSkuRepository.save(sku);
  }

  private void validateSlugUnique(String slug, Long productId) {
    boolean exists = productId == null
        ? productRepository.existsBySlug(slug)
        : productRepository.existsBySlugAndIdNot(slug, productId);
    if (exists) {
      throw new BusinessException(ErrorCode.PRODUCT_SLUG_EXISTS);
    }
  }

  private void validateSkuCodeUnique(String skuCode) {
    if (productSkuRepository.existsBySkuCode(skuCode)) {
      throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "SKU code already exists");
    }
  }

  private void validateStock(Integer stock, Integer lockedStock) {
    if (stock == null || stock < 0) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST, "Stock must be greater than or equal to 0");
    }
    if (lockedStock != null && lockedStock < 0) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST, "Locked stock must be greater than or equal to 0");
    }
    if (lockedStock != null && stock < lockedStock) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST, "Stock cannot be lower than locked stock");
    }
  }

  private String normalizeStatus(String status, Set<String> allowedStatuses, String message) {
    if (status == null || status.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST, message);
    }
    String normalizedStatus = status.trim().toUpperCase();
    if (!allowedStatuses.contains(normalizedStatus)) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST, message);
    }
    return normalizedStatus;
  }
}
