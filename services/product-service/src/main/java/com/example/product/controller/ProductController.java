package com.example.product.controller;

import com.example.product.pojo.Product;
import com.example.product.pojo.ProductSku;
import com.example.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product", description = "Product and SKU management APIs")
public class ProductController {
  private final ProductService productService;

  public ProductController(ProductService productService) {
    this.productService = productService;
  }

  @GetMapping
  @Operation(summary = "List products", description = "List products and optionally filter by product status")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Query successful")
  })
  public List<ProductSummary> listProducts(@RequestParam(required = false) String status) {
    return productService.listProducts(status).stream()
        .map(this::toSummary)
        .toList();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get product detail", description = "Fetch one product with its SKU list")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Query successful"),
      @ApiResponse(responseCode = "404", description = "Product not found")
  })
  public ProductDetail getProduct(@PathVariable Long id) {
    return buildDetail(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  @Operation(summary = "Create product", description = "Create a product and its default SKU")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Created"),
      @ApiResponse(responseCode = "400", description = "Invalid request"),
      @ApiResponse(responseCode = "409", description = "Slug or SKU code already exists")
  })
  public ProductDetail createProduct(@Valid @RequestBody CreateProductRequest request) {
    Product product = new Product();
    product.setName(request.name());
    product.setSlug(request.slug());
    product.setCategoryId(request.categoryId() == null ? 0L : request.categoryId());
    product.setBrand(request.brand());
    product.setDescription(request.description());
    product.setCoverImage(request.coverImage());
    product.setStatus("ON_SALE");

    ProductSku sku = new ProductSku();
    sku.setSkuCode(request.skuCode());
    sku.setSpecJson(request.specJson());
    sku.setSalePrice(request.salePrice());
    sku.setMarketPrice(request.marketPrice());
    sku.setStock(request.stock());
    sku.setLockedStock(0);
    sku.setStatus("ON_SALE");

    Product created = productService.createProduct(product, sku);
    return buildDetail(created.getId());
  }

  @PutMapping("/{id}")
  @Transactional
  @Operation(summary = "Update product", description = "Update product base information")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Updated"),
      @ApiResponse(responseCode = "400", description = "Invalid request"),
      @ApiResponse(responseCode = "404", description = "Product not found"),
      @ApiResponse(responseCode = "409", description = "Slug already exists")
  })
  public ProductDetail updateProduct(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
    Product product = new Product();
    product.setName(request.name());
    product.setSlug(request.slug());
    product.setCategoryId(request.categoryId() == null ? 0L : request.categoryId());
    product.setBrand(request.brand());
    product.setDescription(request.description());
    product.setCoverImage(request.coverImage());
    product.setStatus(request.status());
    productService.updateProduct(id, product);
    return buildDetail(id);
  }

  @PutMapping("/{id}/status")
  @Transactional
  @Operation(summary = "Update product status", description = "Change product status for on-sale or off-shelf operations")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Updated"),
      @ApiResponse(responseCode = "400", description = "Invalid request"),
      @ApiResponse(responseCode = "404", description = "Product not found")
  })
  public ProductDetail updateProductStatus(@PathVariable Long id, @Valid @RequestBody UpdateProductStatusRequest request) {
    productService.updateProductStatus(id, request.status());
    return buildDetail(id);
  }

  @PostMapping("/{id}/skus")
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  @Operation(summary = "Add SKU", description = "Add a new SKU to an existing product")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Created"),
      @ApiResponse(responseCode = "400", description = "Invalid request"),
      @ApiResponse(responseCode = "404", description = "Product not found"),
      @ApiResponse(responseCode = "409", description = "SKU code already exists")
  })
  public ProductSkuResponse addSku(@PathVariable Long id, @Valid @RequestBody CreateProductSkuRequest request) {
    ProductSku sku = new ProductSku();
    sku.setSkuCode(request.skuCode());
    sku.setSpecJson(request.specJson());
    sku.setSalePrice(request.salePrice());
    sku.setMarketPrice(request.marketPrice());
    sku.setStock(request.stock());
    sku.setLockedStock(request.lockedStock() == null ? 0 : request.lockedStock());
    sku.setStatus(request.status() == null ? "ON_SALE" : request.status());
    return toSkuResponse(productService.addSku(id, sku));
  }

  @PutMapping("/{productId}/skus/{skuId}/inventory")
  @Transactional
  @Operation(summary = "Update SKU inventory", description = "Update SKU stock by absolute value or delta and optionally change status")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Updated"),
      @ApiResponse(responseCode = "400", description = "Invalid request"),
      @ApiResponse(responseCode = "404", description = "Product or SKU not found")
  })
  public ProductSkuResponse updateSkuInventory(
      @PathVariable Long productId,
      @PathVariable Long skuId,
      @Valid @RequestBody UpdateSkuInventoryRequest request) {
    return toSkuResponse(productService.updateSkuInventory(
        productId, skuId, request.stock(), request.stockDelta(), request.status()));
  }

  private ProductDetail buildDetail(Long productId) {
    Product product = productService.getProduct(productId);
    List<ProductSkuResponse> skus = productService.getProductSkus(productId).stream()
        .map(this::toSkuResponse)
        .toList();
    return new ProductDetail(
        product.getId(),
        product.getName(),
        product.getSlug(),
        product.getCategoryId(),
        product.getDescription(),
        product.getBrand(),
        product.getCoverImage(),
        product.getStatus(),
        product.getCreatedAt(),
        product.getUpdatedAt(),
        skus);
  }

  private ProductSummary toSummary(Product product) {
    return new ProductSummary(
        product.getId(),
        product.getName(),
        product.getSlug(),
        product.getCategoryId(),
        product.getDescription(),
        product.getCoverImage(),
        product.getStatus(),
        product.getUpdatedAt());
  }

  private ProductSkuResponse toSkuResponse(ProductSku sku) {
    return new ProductSkuResponse(
        sku.getId(),
        sku.getSkuCode(),
        sku.getSpecJson(),
        sku.getSalePrice(),
        sku.getMarketPrice(),
        sku.getStock(),
        sku.getLockedStock(),
        sku.getStatus(),
        sku.getUpdatedAt());
  }

  public record CreateProductRequest(
      @Schema(description = "Product name", example = "Mechanical Keyboard")
      @NotBlank String name,
      @Schema(description = "Unique product slug", example = "mechanical-keyboard")
      @NotBlank String slug,
      @Schema(description = "Category ID", example = "1001")
      Long categoryId,
      @Schema(description = "Brand", example = "KeyPro")
      String brand,
      @Schema(description = "Product description", example = "A hot-swappable mechanical keyboard.")
      @NotBlank String description,
      @Schema(description = "Cover image URL", example = "https://cdn.example.com/p/keyboard.png")
      String coverImage,
      @Schema(description = "SKU code", example = "KB-001")
      @NotBlank String skuCode,
      @Schema(description = "SKU spec JSON", example = "{\"color\":\"black\",\"layout\":\"87\"}")
      @NotBlank String specJson,
      @Schema(description = "Sale price", example = "399.00")
      @NotNull BigDecimal salePrice,
      @Schema(description = "Market price", example = "499.00")
      BigDecimal marketPrice,
      @Schema(description = "Stock", example = "50")
      @NotNull Integer stock) {}

  public record UpdateProductRequest(
      @Schema(description = "Product name", example = "Mechanical Keyboard Pro")
      @NotBlank String name,
      @Schema(description = "Unique product slug", example = "mechanical-keyboard-pro")
      @NotBlank String slug,
      @Schema(description = "Category ID", example = "1001")
      Long categoryId,
      @Schema(description = "Brand", example = "KeyPro")
      String brand,
      @Schema(description = "Product description", example = "Updated product description")
      @NotBlank String description,
      @Schema(description = "Cover image URL", example = "https://cdn.example.com/p/keyboard-pro.png")
      String coverImage,
      @Schema(description = "Product status", example = "ON_SALE")
      String status) {}

  public record UpdateProductStatusRequest(
      @Schema(description = "Product status", example = "OFF_SHELF")
      @NotBlank String status) {}

  public record CreateProductSkuRequest(
      @Schema(description = "SKU code", example = "KB-002")
      @NotBlank String skuCode,
      @Schema(description = "SKU spec JSON", example = "{\"color\":\"white\",\"layout\":\"108\"}")
      @NotBlank String specJson,
      @Schema(description = "Sale price", example = "429.00")
      @NotNull BigDecimal salePrice,
      @Schema(description = "Market price", example = "529.00")
      BigDecimal marketPrice,
      @Schema(description = "Available stock", example = "30")
      @NotNull Integer stock,
      @Schema(description = "Locked stock", example = "0")
      Integer lockedStock,
      @Schema(description = "SKU status", example = "ON_SALE")
      String status) {}

  public record UpdateSkuInventoryRequest(
      @Schema(description = "Set stock to an absolute value", example = "80")
      Integer stock,
      @Schema(description = "Adjust stock by a delta", example = "-2")
      Integer stockDelta,
      @Schema(description = "Optional SKU status", example = "OFF_SHELF")
      String status) {}

  public record ProductSummary(
      @Schema(description = "Product ID", example = "1") Long id,
      @Schema(description = "Product name", example = "Mechanical Keyboard") String name,
      @Schema(description = "Product slug", example = "mechanical-keyboard") String slug,
      @Schema(description = "Category ID", example = "1001") Long categoryId,
      @Schema(description = "Product description") String description,
      @Schema(description = "Cover image URL") String coverImage,
      @Schema(description = "Product status", example = "ON_SALE") String status,
      @Schema(description = "Last updated time") LocalDateTime updatedAt) {}

  public record ProductSkuResponse(
      @Schema(description = "SKU ID", example = "1") Long id,
      @Schema(description = "SKU code", example = "KB-001") String skuCode,
      @Schema(description = "SKU spec JSON") String specJson,
      @Schema(description = "Sale price", example = "399.00") BigDecimal salePrice,
      @Schema(description = "Market price", example = "499.00") BigDecimal marketPrice,
      @Schema(description = "Available stock", example = "50") Integer stock,
      @Schema(description = "Locked stock", example = "0") Integer lockedStock,
      @Schema(description = "SKU status", example = "ON_SALE") String status,
      @Schema(description = "Last updated time") LocalDateTime updatedAt) {}

  public record ProductDetail(
      @Schema(description = "Product ID", example = "1") Long id,
      @Schema(description = "Product name", example = "Mechanical Keyboard") String name,
      @Schema(description = "Product slug", example = "mechanical-keyboard") String slug,
      @Schema(description = "Category ID", example = "1001") Long categoryId,
      @Schema(description = "Product description") String description,
      @Schema(description = "Brand", example = "KeyPro") String brand,
      @Schema(description = "Cover image URL") String coverImage,
      @Schema(description = "Product status", example = "ON_SALE") String status,
      @Schema(description = "Created time") LocalDateTime createdAt,
      @Schema(description = "Last updated time") LocalDateTime updatedAt,
      @Schema(description = "SKU list") List<ProductSkuResponse> skus) {}
}
