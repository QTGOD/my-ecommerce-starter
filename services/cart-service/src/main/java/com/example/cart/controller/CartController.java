package com.example.cart.controller;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "Cart", description = "购物车读写接口")
public class CartController {
  private final Map<Long, List<CartItem>> carts = new ConcurrentHashMap<>();

  @GetMapping
  @Operation(summary = "查询购物车", description = "按用户 ID 查询当前购物车内容")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "查询成功")
  })
  public CartResponse getCart(@RequestParam Long userId) {
    return new CartResponse(userId, new ArrayList<>(carts.getOrDefault(userId, List.of())));
  }

  @PostMapping("/items")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "加入购物车", description = "新增或覆盖同一个 SKU 的购物车项")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "加入成功"),
      @ApiResponse(responseCode = "400", description = "请求参数不合法")
  })
  public CartResponse addItem(@Valid @RequestBody AddCartItemRequest request) {
    List<CartItem> items = new ArrayList<>(carts.getOrDefault(request.userId(), List.of()));
    items.removeIf(item -> item.skuId().equals(request.skuId()));
    items.add(new CartItem(
        request.productId(),
        request.skuId(),
        request.productName(),
        request.unitPrice(),
        request.quantity()));
    carts.put(request.userId(), items);
    return new CartResponse(request.userId(), items);
  }

  @DeleteMapping("/items/{skuId}")
  @Operation(summary = "移除购物车项", description = "按用户 ID 和 SKU ID 删除购物车项")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "移除成功")
  })
  public CartResponse removeItem(@PathVariable Long skuId, @RequestParam Long userId) {
    List<CartItem> items = new ArrayList<>(carts.getOrDefault(userId, List.of()));
    items.removeIf(item -> item.skuId().equals(skuId));
    carts.put(userId, items);
    return new CartResponse(userId, items);
  }

  public record AddCartItemRequest(
      @Schema(description = "用户 ID", example = "1")
      @NotNull Long userId,
      @Schema(description = "商品 ID", example = "1")
      @NotNull Long productId,
      @Schema(description = "SKU ID", example = "1")
      @NotNull Long skuId,
      @Schema(description = "商品名称", example = "Mechanical Keyboard")
      @NotBlank String productName,
      @Schema(description = "单价", example = "399.00")
      @NotNull BigDecimal unitPrice,
      @Schema(description = "数量", example = "1")
      @NotNull Integer quantity) {}

  public record CartItem(
      @Schema(description = "商品 ID", example = "1") Long productId,
      @Schema(description = "SKU ID", example = "1") Long skuId,
      @Schema(description = "商品名称", example = "Mechanical Keyboard") String productName,
      @Schema(description = "单价", example = "399.00") BigDecimal unitPrice,
      @Schema(description = "数量", example = "1") Integer quantity) {}

  public record CartResponse(
      @Schema(description = "用户 ID", example = "1") Long userId,
      @Schema(description = "购物车项") List<CartItem> items) {}
}
