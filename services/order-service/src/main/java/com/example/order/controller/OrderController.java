package com.example.order.controller;

import com.example.common.exception.BusinessException;
import com.example.common.exception.ErrorCode;
import com.example.order.entity.OrderEntity;
import com.example.order.entity.OrderItemEntity;
import com.example.order.entity.dto.OrderOutboxEvent;
import com.example.order.repository.OrderItemRepository;
import com.example.order.repository.OrderOutboxEventRepository;
import com.example.order.repository.OrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order", description = "订单创建与查询接口")
public class OrderController {
  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final OrderOutboxEventRepository outboxEventRepository;
  private final ObjectMapper objectMapper;

  public OrderController(
      OrderRepository orderRepository,
      OrderItemRepository orderItemRepository,
      OrderOutboxEventRepository outboxEventRepository,
      ObjectMapper objectMapper) {
    this.orderRepository = orderRepository;
    this.orderItemRepository = orderItemRepository;
    this.outboxEventRepository = outboxEventRepository;
    this.objectMapper = objectMapper;
  }

  @GetMapping
  @Operation(summary = "订单列表", description = "按用户 ID 查询订单列表")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "查询成功")
  })
  public List<OrderSummary> listOrders(@RequestParam Long userId) {
    return orderRepository.findByUserIdOrderByIdDesc(userId).stream()
        .map(order -> new OrderSummary(order.getId(), order.getOrderNo(), order.getStatus(), order.getPayableAmount()))
        .toList();
  }

  @GetMapping("/{id}")
  @Operation(summary = "订单详情", description = "按订单 ID 查询订单详情")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "查询成功"),
      @ApiResponse(responseCode = "404", description = "订单不存在")
  })
  public OrderDetail getOrder(@PathVariable Long id) {
    OrderEntity order = orderRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    List<OrderItemResponse> items = orderItemRepository.findByOrderId(id).stream()
        .map(item -> new OrderItemResponse(
            item.getProductId(),
            item.getSkuId(),
            item.getProductNameSnapshot(),
            item.getUnitPrice(),
            item.getQuantity(),
            item.getLineTotal()))
        .toList();
    return new OrderDetail(order.getId(), order.getOrderNo(), order.getStatus(), order.getPayableAmount(), items);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  @Operation(summary = "创建订单", description = "创建订单、订单项并写入一条 order.created outbox 事件")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "订单创建成功"),
      @ApiResponse(responseCode = "400", description = "请求参数不合法"),
      @ApiResponse(responseCode = "500", description = "事件序列化失败或服务内部异常")
  })
  public OrderDetail createOrder(@Valid @RequestBody CreateOrderRequest request) {
    OrderEntity existing = orderRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
    if (existing != null) {
      return getOrder(existing.getId());
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

    return getOrder(order.getId());
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      throw new BusinessException(ErrorCode.EVENT_SERIALIZATION_FAILED);
    }
  }

  public record CreateOrderRequest(
      @Schema(description = "用户 ID", example = "1")
      @NotNull Long userId,
      @Schema(description = "幂等键", example = "checkout-20260423-001")
      @NotBlank String idempotencyKey,
      @Schema(description = "订单备注", example = "Please pack carefully")
      String remark,
      @Schema(description = "收货人", example = "Alice")
      @NotBlank String receiverName,
      @Schema(description = "联系电话", example = "13800138000")
      @NotBlank String receiverPhone,
      @Schema(description = "收货地址快照", example = "Hong Kong, Kowloon, Nathan Road 100")
      @NotBlank String receiverAddressSnapshot,
      @Schema(description = "订单项列表")
      @NotEmpty List<CreateOrderItemRequest> items) {}

  public record CreateOrderItemRequest(
      @Schema(description = "商品 ID", example = "1")
      @NotNull Long productId,
      @Schema(description = "SKU ID", example = "1")
      @NotNull Long skuId,
      @Schema(description = "商品名称快照", example = "Mechanical Keyboard")
      @NotBlank String productName,
      @Schema(description = "SKU 规格描述", example = "black / 87-key")
      String skuDesc,
      @Schema(description = "商品图片", example = "https://cdn.example.com/p/keyboard.png")
      String coverImage,
      @Schema(description = "下单单价", example = "399.00")
      @NotNull BigDecimal unitPrice,
      @Schema(description = "购买数量", example = "2")
      @NotNull Integer quantity) {}

  public record OrderSummary(
      @Schema(description = "订单 ID", example = "1") Long id,
      @Schema(description = "订单号", example = "ORD-AB12CD34") String orderNo,
      @Schema(description = "订单状态", example = "CREATED") String status,
      @Schema(description = "应付金额", example = "798.00") BigDecimal payableAmount) {}

  public record OrderItemResponse(
      @Schema(description = "商品 ID", example = "1") Long productId,
      @Schema(description = "SKU ID", example = "1") Long skuId,
      @Schema(description = "商品名称", example = "Mechanical Keyboard") String productName,
      @Schema(description = "单价", example = "399.00") BigDecimal unitPrice,
      @Schema(description = "数量", example = "2") Integer quantity,
      @Schema(description = "小计", example = "798.00") BigDecimal lineTotal) {}

  public record OrderDetail(
      @Schema(description = "订单 ID", example = "1") Long id,
      @Schema(description = "订单号", example = "ORD-AB12CD34") String orderNo,
      @Schema(description = "订单状态", example = "CREATED") String status,
      @Schema(description = "应付金额", example = "798.00") BigDecimal payableAmount,
      @Schema(description = "订单项") List<OrderItemResponse> items) {}

  public record OrderCreatedPayload(
      @Schema(description = "订单 ID", example = "1") Long orderId,
      @Schema(description = "用户 ID", example = "1") Long userId,
      @Schema(description = "订单金额", example = "798.00") BigDecimal amount) {}
}
