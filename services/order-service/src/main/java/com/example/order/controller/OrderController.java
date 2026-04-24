package com.example.order.controller;

import com.example.common.exception.BusinessException;
import com.example.common.exception.ErrorCode;
import com.example.order.entity.OrderEntity;
import com.example.order.entity.OrderItemEntity;
import com.example.order.entity.OrderStatusLogEntity;
import com.example.order.entity.dto.OrderOutboxEvent;
import com.example.order.repository.OrderItemRepository;
import com.example.order.repository.OrderOutboxEventRepository;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.OrderStatusLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order", description = "Order create, query, cancel, and status management APIs")
public class OrderController {
  private static final Set<String> ALLOWED_ORDER_STATUSES =
      Set.of("CREATED", "PAID", "SHIPPED", "COMPLETED", "CANCELLED", "CLOSED");
  private static final Set<String> CANCELLABLE_STATUSES = Set.of("CREATED", "PAID");

  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final OrderOutboxEventRepository outboxEventRepository;
  private final OrderStatusLogRepository orderStatusLogRepository;
  private final ObjectMapper objectMapper;

  public OrderController(
      OrderRepository orderRepository,
      OrderItemRepository orderItemRepository,
      OrderOutboxEventRepository outboxEventRepository,
      OrderStatusLogRepository orderStatusLogRepository,
      ObjectMapper objectMapper) {
    this.orderRepository = orderRepository;
    this.orderItemRepository = orderItemRepository;
    this.outboxEventRepository = outboxEventRepository;
    this.orderStatusLogRepository = orderStatusLogRepository;
    this.objectMapper = objectMapper;
  }

  @GetMapping
  @Operation(summary = "List orders", description = "Query orders by user and optional status")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "Query succeeded")})
  public List<OrderSummary> listOrders(@RequestParam Long userId, @RequestParam(required = false) String status) {
    List<OrderEntity> orders = status == null || status.isBlank()
        ? orderRepository.findByUserIdOrderByIdDesc(userId)
        : orderRepository.findByUserIdAndStatusOrderByIdDesc(userId, normalizeStatus(status));
    return orders.stream().map(this::toOrderSummary).toList();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get order detail", description = "Query order detail by order id")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Query succeeded"),
      @ApiResponse(responseCode = "404", description = "Order not found")
  })
  public OrderDetail getOrder(@PathVariable Long id) {
    OrderEntity order = getOrderEntity(id);
    return buildOrderDetail(order);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  @Operation(summary = "Create order", description = "Create order, order items, and one order.created outbox event")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Order created"),
      @ApiResponse(responseCode = "400", description = "Invalid request"),
      @ApiResponse(responseCode = "500", description = "Event serialization failed")
  })
  public OrderDetail createOrder(@Valid @RequestBody CreateOrderRequest request) {
    OrderEntity existing = orderRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
    if (existing != null) {
      return buildOrderDetail(existing);
    }

    BigDecimal totalAmount = request.items().stream()
        .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    OrderEntity order = new OrderEntity();
    order.setOrderNo("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    order.setUserId(request.userId());
    order.setStatus("CREATED");
    order.setTotalAmount(totalAmount);
    order.setPayableAmount(totalAmount);
    order.setPaymentStatus("INIT");
    order.setDeliveryStatus("PENDING");
    order.setIdempotencyKey(request.idempotencyKey());
    order.setRemark(request.remark());
    order.setReceiverName(request.receiverName());
    order.setReceiverPhone(request.receiverPhone());
    order.setReceiverAddressSnapshot(request.receiverAddressSnapshot());
    orderRepository.save(order);

    for (CreateOrderItemRequest itemRequest : request.items()) {
      validateOrderItem(itemRequest);

      OrderItemEntity item = new OrderItemEntity();
      item.setOrderId(order.getId());
      item.setProductId(itemRequest.productId());
      item.setSkuId(itemRequest.skuId());
      item.setProductNameSnapshot(itemRequest.productName());
      item.setSkuDescSnapshot(itemRequest.skuDesc());
      item.setCoverImageSnapshot(itemRequest.coverImage());
      item.setUnitPrice(itemRequest.unitPrice());
      item.setQuantity(itemRequest.quantity());
      item.setLineTotal(itemRequest.unitPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())));
      orderItemRepository.save(item);
    }

    OrderOutboxEvent event = new OrderOutboxEvent();
    event.setAggregateType("ORDER");
    event.setAggregateId(order.getId());
    event.setEventType("order.created");
    event.setPayloadJson(writeJson(new OrderCreatedPayload(order.getId(), order.getUserId(), order.getPayableAmount())));
    event.setStatus("NEW");
    outboxEventRepository.save(event);

    appendStatusLog(order.getId(), "INIT", order.getStatus(), "system", "Order created");
    return buildOrderDetail(order);
  }

  @PostMapping("/{id}/cancel")
  @Transactional
  @Operation(summary = "Cancel order", description = "Cancel an order while it is still cancellable")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Order cancelled"),
      @ApiResponse(responseCode = "400", description = "Current order state does not allow cancellation"),
      @ApiResponse(responseCode = "404", description = "Order not found")
  })
  public OrderDetail cancelOrder(@PathVariable Long id, @Valid @RequestBody CancelOrderRequest request) {
    OrderEntity order = getOrderEntity(id);
    if (!CANCELLABLE_STATUSES.contains(order.getStatus())) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST, "Current order status does not allow cancellation");
    }

    String previousStatus = order.getStatus();
    order.setStatus("CANCELLED");
    if ("INIT".equals(order.getPaymentStatus())) {
      order.setPaymentStatus("CANCELLED");
    }
    orderRepository.save(order);
    appendStatusLog(order.getId(), previousStatus, order.getStatus(), request.operator(), request.remark());
    return buildOrderDetail(order);
  }

  @PatchMapping("/{id}/status")
  @Transactional
  @Operation(summary = "Update order status", description = "Update order status and optional payment or delivery status")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Order status updated"),
      @ApiResponse(responseCode = "400", description = "Invalid status transition"),
      @ApiResponse(responseCode = "404", description = "Order not found")
  })
  public OrderDetail updateOrderStatus(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
    OrderEntity order = getOrderEntity(id);
    String targetStatus = normalizeStatus(request.status());
    validateOrderStatusTransition(order.getStatus(), targetStatus);

    String previousStatus = order.getStatus();
    order.setStatus(targetStatus);

    if (request.paymentStatus() != null && !request.paymentStatus().isBlank()) {
      order.setPaymentStatus(normalizeStatus(request.paymentStatus()));
    } else if ("PAID".equals(targetStatus)) {
      order.setPaymentStatus("SUCCESS");
    } else if ("CANCELLED".equals(targetStatus)) {
      order.setPaymentStatus("CANCELLED");
    }

    if (request.deliveryStatus() != null && !request.deliveryStatus().isBlank()) {
      order.setDeliveryStatus(normalizeStatus(request.deliveryStatus()));
    } else if ("SHIPPED".equals(targetStatus)) {
      order.setDeliveryStatus("SHIPPED");
    } else if ("COMPLETED".equals(targetStatus)) {
      order.setDeliveryStatus("DELIVERED");
    }

    if (request.remark() != null && !request.remark().isBlank()) {
      order.setRemark(request.remark());
    }

    orderRepository.save(order);
    appendStatusLog(order.getId(), previousStatus, targetStatus, request.operator(), request.remark());
    return buildOrderDetail(order);
  }

  private OrderEntity getOrderEntity(Long id) {
    return orderRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
  }

  private OrderDetail buildOrderDetail(OrderEntity order) {
    List<OrderItemResponse> items = orderItemRepository.findByOrderId(order.getId()).stream()
        .map(item -> new OrderItemResponse(
            item.getProductId(),
            item.getSkuId(),
            item.getProductNameSnapshot(),
            item.getUnitPrice(),
            item.getQuantity(),
            item.getLineTotal()))
        .toList();
    List<OrderStatusLogResponse> statusLogs = orderStatusLogRepository.findByOrderIdOrderByIdDesc(order.getId()).stream()
        .map(log -> new OrderStatusLogResponse(
            log.getFromStatus(),
            log.getToStatus(),
            log.getOperator(),
            log.getRemark(),
            log.getCreatedAt()))
        .toList();
    return new OrderDetail(
        order.getId(),
        order.getOrderNo(),
        order.getStatus(),
        order.getPaymentStatus(),
        order.getDeliveryStatus(),
        order.getPayableAmount(),
        order.getRemark(),
        order.getCreatedAt(),
        items,
        statusLogs);
  }

  private OrderSummary toOrderSummary(OrderEntity order) {
    return new OrderSummary(
        order.getId(),
        order.getOrderNo(),
        order.getStatus(),
        order.getPaymentStatus(),
        order.getDeliveryStatus(),
        order.getPayableAmount(),
        order.getCreatedAt());
  }

  private void validateOrderItem(CreateOrderItemRequest item) {
    if (item.quantity() == null || item.quantity() <= 0) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST, "Order item quantity must be greater than 0");
    }
    if (item.unitPrice() == null || item.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST, "Order item unit price must not be negative");
    }
  }

  private void validateOrderStatusTransition(String currentStatus, String targetStatus) {
    if (!ALLOWED_ORDER_STATUSES.contains(targetStatus)) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST, "Unsupported order status: " + targetStatus);
    }
    if (currentStatus.equals(targetStatus)) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST, "Order status is already " + targetStatus);
    }
    boolean valid = switch (currentStatus) {
      case "CREATED" -> Set.of("PAID", "CANCELLED", "CLOSED").contains(targetStatus);
      case "PAID" -> Set.of("SHIPPED", "CANCELLED", "COMPLETED").contains(targetStatus);
      case "SHIPPED" -> Set.of("COMPLETED").contains(targetStatus);
      case "COMPLETED", "CANCELLED", "CLOSED" -> false;
      default -> false;
    };
    if (!valid) {
      throw new BusinessException(
          ErrorCode.INVALID_REQUEST,
          "Invalid order status transition from " + currentStatus + " to " + targetStatus);
    }
  }

  private String normalizeStatus(String value) {
    return value.trim().toUpperCase();
  }

  private void appendStatusLog(Long orderId, String fromStatus, String toStatus, String operator, String remark) {
    OrderStatusLogEntity statusLog = new OrderStatusLogEntity();
    statusLog.setOrderId(orderId);
    statusLog.setFromStatus(fromStatus);
    statusLog.setToStatus(toStatus);
    statusLog.setOperator(operator);
    statusLog.setRemark(remark);
    orderStatusLogRepository.save(statusLog);
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      throw new BusinessException(ErrorCode.EVENT_SERIALIZATION_FAILED);
    }
  }

  public record CreateOrderRequest(
      @Schema(description = "User id", example = "1")
      @NotNull Long userId,
      @Schema(description = "Idempotency key", example = "checkout-20260423-001")
      @NotBlank String idempotencyKey,
      @Schema(description = "Order remark", example = "Please pack carefully")
      String remark,
      @Schema(description = "Receiver name", example = "Alice")
      @NotBlank String receiverName,
      @Schema(description = "Receiver phone", example = "13800138000")
      @NotBlank String receiverPhone,
      @Schema(description = "Receiver address snapshot", example = "Hong Kong, Kowloon, Nathan Road 100")
      @NotBlank String receiverAddressSnapshot,
      @Schema(description = "Order items")
      @NotEmpty List<CreateOrderItemRequest> items) {}

  public record CreateOrderItemRequest(
      @Schema(description = "Product id", example = "1")
      @NotNull Long productId,
      @Schema(description = "Sku id", example = "1")
      @NotNull Long skuId,
      @Schema(description = "Product name snapshot", example = "Mechanical Keyboard")
      @NotBlank String productName,
      @Schema(description = "Sku description snapshot", example = "black / 87-key")
      String skuDesc,
      @Schema(description = "Cover image", example = "https://cdn.example.com/p/keyboard.png")
      String coverImage,
      @Schema(description = "Order item unit price", example = "399.00")
      @NotNull BigDecimal unitPrice,
      @Schema(description = "Order item quantity", example = "2")
      @NotNull Integer quantity) {}

  public record CancelOrderRequest(
      @Schema(description = "Operator", example = "customer")
      @NotBlank String operator,
      @Schema(description = "Cancellation remark", example = "User requested cancellation")
      String remark) {}

  public record UpdateOrderStatusRequest(
      @Schema(description = "Target order status", example = "PAID")
      @NotBlank String status,
      @Schema(description = "Operator", example = "system")
      @NotBlank String operator,
      @Schema(description = "Optional payment status", example = "SUCCESS")
      String paymentStatus,
      @Schema(description = "Optional delivery status", example = "SHIPPED")
      String deliveryStatus,
      @Schema(description = "Status update remark", example = "Payment callback received")
      String remark) {}

  public record OrderSummary(
      @Schema(description = "Order id", example = "1") Long id,
      @Schema(description = "Order number", example = "ORD-AB12CD34") String orderNo,
      @Schema(description = "Order status", example = "CREATED") String status,
      @Schema(description = "Payment status", example = "INIT") String paymentStatus,
      @Schema(description = "Delivery status", example = "PENDING") String deliveryStatus,
      @Schema(description = "Payable amount", example = "798.00") BigDecimal payableAmount,
      @Schema(description = "Created time") LocalDateTime createdAt) {}

  public record OrderItemResponse(
      @Schema(description = "Product id", example = "1") Long productId,
      @Schema(description = "Sku id", example = "1") Long skuId,
      @Schema(description = "Product name", example = "Mechanical Keyboard") String productName,
      @Schema(description = "Unit price", example = "399.00") BigDecimal unitPrice,
      @Schema(description = "Quantity", example = "2") Integer quantity,
      @Schema(description = "Line total", example = "798.00") BigDecimal lineTotal) {}

  public record OrderStatusLogResponse(
      @Schema(description = "Previous status", example = "CREATED") String fromStatus,
      @Schema(description = "Current status", example = "PAID") String toStatus,
      @Schema(description = "Operator", example = "system") String operator,
      @Schema(description = "Remark", example = "Payment callback received") String remark,
      @Schema(description = "Change time") LocalDateTime createdAt) {}

  public record OrderDetail(
      @Schema(description = "Order id", example = "1") Long id,
      @Schema(description = "Order number", example = "ORD-AB12CD34") String orderNo,
      @Schema(description = "Order status", example = "CREATED") String status,
      @Schema(description = "Payment status", example = "INIT") String paymentStatus,
      @Schema(description = "Delivery status", example = "PENDING") String deliveryStatus,
      @Schema(description = "Payable amount", example = "798.00") BigDecimal payableAmount,
      @Schema(description = "Remark", example = "Please pack carefully") String remark,
      @Schema(description = "Created time") LocalDateTime createdAt,
      @Schema(description = "Order items") List<OrderItemResponse> items,
      @Schema(description = "Order status logs") List<OrderStatusLogResponse> statusLogs) {}

  public record OrderCreatedPayload(
      @Schema(description = "Order id", example = "1") Long orderId,
      @Schema(description = "User id", example = "1") Long userId,
      @Schema(description = "Order amount", example = "798.00") BigDecimal amount) {}
}
