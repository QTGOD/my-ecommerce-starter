package com.example.cart.controller;

import com.example.cart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/cart")
@Tag(name = "Cart", description = "Shopping cart read and write APIs")
public class CartController {
  private final CartService cartService;

  public CartController(CartService cartService) {
    this.cartService = cartService;
  }

  @GetMapping
  @Operation(summary = "Get cart", description = "Query current cart items and totals by user ID")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Query success"),
      @ApiResponse(responseCode = "400", description = "User ID invalid")
  })
  public CartResponse getCart(@RequestParam Long userId) {
    return toResponse(cartService.getCart(userId));
  }

  @PostMapping("/items")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Add cart item", description = "Add an item, and merge quantity if the SKU already exists")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Add success"),
      @ApiResponse(responseCode = "400", description = "Request parameters invalid")
  })
  public CartResponse addItem(@Valid @RequestBody AddCartItemRequest request) {
    return toResponse(cartService.addItem(new CartService.AddItemCommand(
        request.userId(),
        request.productId(),
        request.skuId(),
        request.productName(),
        request.unitPrice(),
        request.quantity())));
  }

  @PutMapping("/items/{skuId}")
  @Operation(summary = "Update quantity", description = "Update an existing cart item quantity")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Update success"),
      @ApiResponse(responseCode = "400", description = "Request parameters invalid")
  })
  public CartResponse updateItemQuantity(
      @PathVariable Long skuId,
      @RequestParam Long userId,
      @Valid @RequestBody UpdateCartItemQuantityRequest request) {
    return toResponse(cartService.updateQuantity(userId, skuId, request.quantity()));
  }

  @DeleteMapping("/items/{skuId}")
  @Operation(summary = "Remove cart item", description = "Delete a cart item by user ID and SKU ID")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Remove success")
  })
  public CartResponse removeItem(@PathVariable Long skuId, @RequestParam Long userId) {
    return toResponse(cartService.removeItem(userId, skuId));
  }

  @DeleteMapping
  @Operation(summary = "Clear cart", description = "Clear all items for a user")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Clear success")
  })
  public CartResponse clearCart(@RequestParam Long userId) {
    return toResponse(cartService.clearCart(userId));
  }

  private CartResponse toResponse(CartService.CartView view) {
    List<CartItemResponse> items = view.items().stream()
        .map(item -> new CartItemResponse(
            item.productId(),
            item.skuId(),
            item.productName(),
            item.unitPrice(),
            item.quantity(),
            item.lineAmount()))
        .toList();

    return new CartResponse(
        view.userId(),
        items,
        view.itemCount(),
        view.totalQuantity(),
        view.totalAmount());
  }

  public record AddCartItemRequest(
      @Schema(description = "User ID", example = "1")
      @NotNull @Positive Long userId,
      @Schema(description = "Product ID", example = "1")
      @NotNull @Positive Long productId,
      @Schema(description = "SKU ID", example = "1")
      @NotNull @Positive Long skuId,
      @Schema(description = "Product name", example = "Mechanical Keyboard")
      @NotBlank String productName,
      @Schema(description = "Unit price", example = "399.00")
      @NotNull @DecimalMin("0.00") BigDecimal unitPrice,
      @Schema(description = "Quantity", example = "1")
      @NotNull @Positive Integer quantity) {}

  public record UpdateCartItemQuantityRequest(
      @Schema(description = "Quantity", example = "3")
      @NotNull @Positive Integer quantity) {}

  public record CartItemResponse(
      @Schema(description = "Product ID", example = "1") Long productId,
      @Schema(description = "SKU ID", example = "1") Long skuId,
      @Schema(description = "Product name", example = "Mechanical Keyboard") String productName,
      @Schema(description = "Unit price", example = "399.00") BigDecimal unitPrice,
      @Schema(description = "Quantity", example = "1") Integer quantity,
      @Schema(description = "Line amount", example = "399.00") BigDecimal lineAmount) {}

  public record CartResponse(
      @Schema(description = "User ID", example = "1") Long userId,
      @Schema(description = "Cart items") List<CartItemResponse> items,
      @Schema(description = "Number of distinct cart items", example = "2") Integer itemCount,
      @Schema(description = "Total quantity", example = "3") Integer totalQuantity,
      @Schema(description = "Total amount", example = "798.00") BigDecimal totalAmount) {}
}
