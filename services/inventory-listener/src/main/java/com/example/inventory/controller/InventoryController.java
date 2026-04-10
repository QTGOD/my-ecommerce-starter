package com.example.inventory.controller;

import com.example.common.exception.BusinessException;
import com.example.common.exception.ErrorCode;
import com.example.inventory.entity.ConsumedMessageEntity;
import com.example.inventory.entity.InventoryReservationEntity;
import com.example.inventory.repository.ConsumedMessageRepository;
import com.example.inventory.repository.InventoryReservationRepository;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/api/inventory")
@Tag(name = "Inventory", description = "库存预留与消息幂等记录接口")
public class InventoryController {
  private final InventoryReservationRepository inventoryReservationRepository;
  private final ConsumedMessageRepository consumedMessageRepository;

  public InventoryController(
      InventoryReservationRepository inventoryReservationRepository,
      ConsumedMessageRepository consumedMessageRepository) {
    this.inventoryReservationRepository = inventoryReservationRepository;
    this.consumedMessageRepository = consumedMessageRepository;
  }

  @PostMapping("/reservations")
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  @Operation(summary = "创建或更新库存预留记录", description = "按订单写入一条库存预留记录，用于模拟库存预扣结果")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "记录成功"),
      @ApiResponse(responseCode = "400", description = "请求参数不合法")
  })
  public ReservationResponse reserve(@Valid @RequestBody ReserveRequest request) {
    InventoryReservationEntity reservation = inventoryReservationRepository.findByOrderId(request.orderId())
        .orElseGet(InventoryReservationEntity::new);
    reservation.setOrderId(request.orderId());
    reservation.setUserId(request.userId());
    reservation.setStatus(request.status());
    inventoryReservationRepository.save(reservation);
    return new ReservationResponse(reservation.getId(), reservation.getOrderId(), reservation.getStatus());
  }

  @GetMapping("/reservations/{orderId}")
  @Operation(summary = "查询库存预留记录", description = "按订单 ID 查询库存预留状态")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "查询成功"),
      @ApiResponse(responseCode = "404", description = "预留记录不存在")
  })
  public ReservationResponse getReservation(@PathVariable Long orderId) {
    InventoryReservationEntity reservation = inventoryReservationRepository.findByOrderId(orderId)
        .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_RESERVATION_NOT_FOUND));
    return new ReservationResponse(reservation.getId(), reservation.getOrderId(), reservation.getStatus());
  }

  @PostMapping("/messages/consume")
  @Transactional
  @Operation(summary = "记录消息消费", description = "写入消费记录，重复 messageId + consumerName 会返回 DUPLICATE")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "记录成功或识别为重复消息"),
      @ApiResponse(responseCode = "400", description = "请求参数不合法")
  })
  public MessageConsumeResponse markConsumed(@Valid @RequestBody ConsumeMessageRequest request) {
    boolean duplicate = consumedMessageRepository.existsByMessageIdAndConsumerName(
        request.messageId(), request.consumerName());
    if (!duplicate) {
      ConsumedMessageEntity message = new ConsumedMessageEntity();
      message.setMessageId(request.messageId());
      message.setConsumerName(request.consumerName());
      message.setEventType(request.eventType());
      consumedMessageRepository.save(message);
    }
    return new MessageConsumeResponse(request.messageId(), duplicate ? "DUPLICATE" : "RECORDED");
  }

  public record ReserveRequest(
      @Schema(description = "订单 ID", example = "1") @NotNull Long orderId,
      @Schema(description = "用户 ID", example = "1") @NotNull Long userId,
      @Schema(description = "预留状态", example = "RESERVED") @NotBlank String status) {}

  public record ReservationResponse(
      @Schema(description = "预留记录 ID", example = "1") Long id,
      @Schema(description = "订单 ID", example = "1") Long orderId,
      @Schema(description = "状态", example = "RESERVED") String status) {}

  public record ConsumeMessageRequest(
      @Schema(description = "消息 ID", example = "msg-20260423-001") @NotBlank String messageId,
      @Schema(description = "消费者名称", example = "inventory-listener") @NotBlank String consumerName,
      @Schema(description = "事件类型", example = "order.created") @NotBlank String eventType) {}

  public record MessageConsumeResponse(
      @Schema(description = "消息 ID", example = "msg-20260423-001") String messageId,
      @Schema(description = "处理结果", example = "RECORDED") String result) {}
}
