package com.example.product.controller;

import com.example.common.exception.BusinessException;
import com.example.common.exception.ErrorCode;
import com.example.product.repository.ProductRepository;
import com.example.product.repository.ProductSkuRepository;
import com.example.product.pojo.Product;
import com.example.product.pojo.ProductSku;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product", description = "商品与 SKU 管理接口")
public class ProductController {
  private final ProductRepository productRepository;
  private final ProductSkuRepository productSkuRepository;

  public ProductController(ProductRepository productRepository, ProductSkuRepository productSkuRepository) {
    this.productRepository = productRepository;
    this.productSkuRepository = productSkuRepository;
  }

  @GetMapping
  @Operation(summary = "商品列表", description = "查询当前所有商品的基础信息")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "查询成功")
  })
  public List<ProductSummary> listProducts() {
    return productRepository.findAll().stream()
        .map(product -> new ProductSummary(
            product.getId(),
            product.getName(),
            product.getSlug(),
            product.getDescription(),
            product.getCoverImage(),
            product.getStatus()))
        .toList();
  }

  @GetMapping("/{id}")
  @Operation(summary = "商品详情", description = "按商品 ID 查询商品详情及 SKU 列表")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "查询成功"),
      @ApiResponse(responseCode = "404", description = "商品不存在")
  })
  public ProductDetail getProduct(@PathVariable Long id) {
    Product product = productRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    List<ProductSku> skus = productSkuRepository.findByProductId(id);
    return new ProductDetail(
        product.getId(),
        product.getName(),
        product.getSlug(),
        product.getDescription(),
        product.getBrand(),
        product.getCoverImage(),
        product.getStatus(),
        skus.stream()
            .map(sku -> new ProductSkuResponse(
                sku.getId(), sku.getSkuCode(), sku.getSpecJson(), sku.getSalePrice(), sku.getStock(), sku.getStatus()))
            .toList());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  @Operation(summary = "创建商品", description = "创建商品并同时创建一个默认 SKU")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "创建成功"),
      @ApiResponse(responseCode = "400", description = "请求参数不合法"),
      @ApiResponse(responseCode = "409", description = "slug 已存在")
  })
  public ProductDetail createProduct(@Valid @RequestBody CreateProductRequest request) {
    if (productRepository.existsBySlug(request.slug())) {
      throw new BusinessException(ErrorCode.PRODUCT_SLUG_EXISTS);
    }

    Product product = new Product();
    product.setName(request.name());
    product.setSlug(request.slug());
    product.setCategoryId(request.categoryId() == null ? 0L : request.categoryId());
    product.setBrand(request.brand());
    product.setDescription(request.description());
    product.setCoverImage(request.coverImage());
    product.setStatus("ON_SALE");
    productRepository.save(product);

    ProductSku sku = new ProductSku();
    sku.setProductId(product.getId());
    sku.setSkuCode(request.skuCode());
    sku.setSpecJson(request.specJson());
    sku.setSalePrice(request.salePrice());
    sku.setMarketPrice(request.marketPrice());
    sku.setStock(request.stock());
    sku.setLockedStock(0);
    sku.setStatus("ON_SALE");
    productSkuRepository.save(sku);

    return getProduct(product.getId());
  }

  public record CreateProductRequest(
      @Schema(description = "商品名称", example = "Mechanical Keyboard")
      @NotBlank String name,
      @Schema(description = "商品唯一 slug", example = "mechanical-keyboard")
      @NotBlank String slug,
      @Schema(description = "分类 ID", example = "1001")
      Long categoryId,
      @Schema(description = "品牌", example = "KeyPro")
      String brand,
      @Schema(description = "商品描述", example = "A hot-swappable mechanical keyboard.")
      @NotBlank String description,
      @Schema(description = "封面图 URL", example = "https://cdn.example.com/p/keyboard.png")
      String coverImage,
      @Schema(description = "SKU 编码", example = "KB-001")
      @NotBlank String skuCode,
      @Schema(description = "SKU 规格 JSON", example = "{\"color\":\"black\",\"layout\":\"87\"}")
      @NotBlank String specJson,
      @Schema(description = "销售价", example = "399.00")
      @NotNull BigDecimal salePrice,
      @Schema(description = "划线价", example = "499.00")
      BigDecimal marketPrice,
      @Schema(description = "库存", example = "50")
      @NotNull Integer stock) {}

  public record ProductSummary(
      @Schema(description = "商品 ID", example = "1") Long id,
      @Schema(description = "商品名称", example = "Mechanical Keyboard") String name,
      @Schema(description = "商品 slug", example = "mechanical-keyboard") String slug,
      @Schema(description = "商品描述") String description,
      @Schema(description = "封面图 URL") String coverImage,
      @Schema(description = "商品状态", example = "ON_SALE") String status) {}

  public record ProductSkuResponse(
      @Schema(description = "SKU ID", example = "1") Long id,
      @Schema(description = "SKU 编码", example = "KB-001") String skuCode,
      @Schema(description = "SKU 规格 JSON") String specJson,
      @Schema(description = "售价", example = "399.00") BigDecimal salePrice,
      @Schema(description = "库存", example = "50") Integer stock,
      @Schema(description = "SKU 状态", example = "ON_SALE") String status) {}

  public record ProductDetail(
      @Schema(description = "商品 ID", example = "1") Long id,
      @Schema(description = "商品名称", example = "Mechanical Keyboard") String name,
      @Schema(description = "商品 slug", example = "mechanical-keyboard") String slug,
      @Schema(description = "商品描述") String description,
      @Schema(description = "品牌", example = "KeyPro") String brand,
      @Schema(description = "封面图 URL") String coverImage,
      @Schema(description = "状态", example = "ON_SALE") String status,
      @Schema(description = "SKU 列表") List<ProductSkuResponse> skus) {}
}
