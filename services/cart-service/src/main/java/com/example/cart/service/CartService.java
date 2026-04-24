package com.example.cart.service;

import com.example.common.exception.BusinessException;
import com.example.common.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class CartService {
  private final Map<Long, Map<Long, CartItemView>> carts = new ConcurrentHashMap<>();

  public CartView getCart(Long userId) {
    validateUserId(userId);
    return buildCartView(userId);
  }

  public CartView addItem(AddItemCommand command) {
    validateUserId(command.userId());
    validateQuantity(command.quantity());
    validateAmount(command.unitPrice());

    Map<Long, CartItemView> userCart = carts.computeIfAbsent(command.userId(), key -> new ConcurrentHashMap<>());
    userCart.compute(command.skuId(), (skuId, existing) -> {
      if (existing == null) {
        return new CartItemView(
            command.productId(),
            command.skuId(),
            command.productName(),
            command.unitPrice(),
            command.quantity(),
            command.unitPrice().multiply(BigDecimal.valueOf(command.quantity())));
      }

      return buildItem(
          command.productId(),
          command.skuId(),
          command.productName(),
          command.unitPrice(),
          existing.quantity() + command.quantity());
    });
    return buildCartView(command.userId());
  }

  public CartView updateQuantity(Long userId, Long skuId, Integer quantity) {
    validateUserId(userId);
    validateQuantity(quantity);

    Map<Long, CartItemView> userCart = carts.get(userId);
    if (userCart == null || !userCart.containsKey(skuId)) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST, "Cart item does not exist");
    }

    CartItemView existing = userCart.get(skuId);
    userCart.put(skuId, buildItem(
        existing.productId(),
        existing.skuId(),
        existing.productName(),
        existing.unitPrice(),
        quantity));
    return buildCartView(userId);
  }

  public CartView removeItem(Long userId, Long skuId) {
    validateUserId(userId);
    Map<Long, CartItemView> userCart = carts.get(userId);
    if (userCart != null) {
      userCart.remove(skuId);
      if (userCart.isEmpty()) {
        carts.remove(userId);
      }
    }
    return buildCartView(userId);
  }

  public CartView clearCart(Long userId) {
    validateUserId(userId);
    carts.remove(userId);
    return buildCartView(userId);
  }

  private CartView buildCartView(Long userId) {
    List<CartItemView> items = new ArrayList<>(carts.getOrDefault(userId, Map.of()).values());
    items.sort((left, right) -> left.skuId().compareTo(right.skuId()));

    BigDecimal totalAmount = items.stream()
        .map(CartItemView::lineAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    int totalQuantity = items.stream()
        .mapToInt(CartItemView::quantity)
        .sum();

    return new CartView(userId, items, items.size(), totalQuantity, totalAmount);
  }

  private CartItemView buildItem(Long productId, Long skuId, String productName, BigDecimal unitPrice, Integer quantity) {
    return new CartItemView(
        productId,
        skuId,
        productName,
        unitPrice,
        quantity,
        unitPrice.multiply(BigDecimal.valueOf(quantity)));
  }

  private void validateUserId(Long userId) {
    if (userId == null || userId <= 0) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST, "User ID must be greater than 0");
    }
  }

  private void validateQuantity(Integer quantity) {
    if (quantity == null || quantity <= 0) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST, "Quantity must be greater than 0");
    }
  }

  private void validateAmount(BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST, "Unit price must be greater than or equal to 0");
    }
  }

  public record AddItemCommand(
      Long userId,
      Long productId,
      Long skuId,
      String productName,
      BigDecimal unitPrice,
      Integer quantity) {}

  public record CartItemView(
      Long productId,
      Long skuId,
      String productName,
      BigDecimal unitPrice,
      Integer quantity,
      BigDecimal lineAmount) {}

  public record CartView(
      Long userId,
      List<CartItemView> items,
      Integer itemCount,
      Integer totalQuantity,
      BigDecimal totalAmount) {}
}
