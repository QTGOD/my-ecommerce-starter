package com.example.inventory.controller;

import com.example.inventory.entity.InventoryReservationEntity;
import com.example.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/api/inventory")
@Tag(name = "Inventory", description = "Inventory reservation and message consumption APIs")
public class InventoryController {
  private final InventoryService inventoryService;

  public InventoryController(InventoryService inventoryService) {
    this.inventoryService = inventoryService;
  }

  @PostMapping("/reservations")
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  @Operation(summary = "Create or update reservation", description = "Upsert one inventory reservation by order ID")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Saved"),
      @ApiResponse(responseCode = "400", description = "Invalid request")
  })
  public ReservationResponse reserve(@Valid @RequestBody ReserveRequest request) {
    return toResponse(inventoryService.upsertReservation(
        request.orderId(), request.userId(), request.status()));
  }

  @GetMapping("/reservations/{orderId}")
  @Operation(summary = "Get reservation", description = "Query one reservation by order ID")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Query successful"),
      @ApiResponse(responseCode = "404", description = "Reservation not found")
  })
  public ReservationResponse getReservation(@PathVariable Long orderId) {
    return toResponse(inventoryService.getReservation(orderId));
  }

  @GetMapping("/reservations")
  @Operation(summary = "List reservations", description = "List reservations and optionally filter by user ID or status")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Query successful")
  })
  public List<ReservationResponse> listReservations(
      @RequestParam(required = false) Long userId,
      @RequestParam(required = false) String status) {
    return inventoryService.listReservations(userId, status).stream()
        .map(this::toResponse)
        .toList();
  }

  @GetMapping("/users/{userId}/reservations")
  @Operation(summary = "List user reservations", description = "List reservations for one user and optionally filter by status")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Query successful")
  })
  public List<ReservationResponse> listUserReservations(
      @PathVariable Long userId,
      @RequestParam(required = false) String status) {
    return inventoryService.listReservations(userId, status).stream()
        .map(this::toResponse)
        .toList();
  }

  @PutMapping("/reservations/{orderId}/status")
  @Transactional
  @Operation(summary = "Update reservation status", description = "Update one reservation status by order ID")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Updated"),
      @ApiResponse(responseCode = "400", description = "Invalid request"),
      @ApiResponse(responseCode = "404", description = "Reservation not found")
  })
  public ReservationResponse updateReservationStatus(
      @PathVariable Long orderId,
      @Valid @RequestBody UpdateReservationStatusRequest request) {
    return toResponse(inventoryService.updateReservationStatus(orderId, request.status()));
  }

  @PostMapping("/messages/consume")
  @Transactional
  @Operation(summary = "Record consumed message", description = "Record a consumed message and identify duplicates")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Recorded or duplicate identified"),
      @ApiResponse(responseCode = "400", description = "Invalid request")
  })
  public MessageConsumeResponse markConsumed(@Valid @RequestBody ConsumeMessageRequest request) {
    boolean duplicate = inventoryService.markConsumed(
        request.messageId(), request.consumerName(), request.eventType());
    return new MessageConsumeResponse(request.messageId(), duplicate ? "DUPLICATE" : "RECORDED");
  }

  private ReservationResponse toResponse(InventoryReservationEntity reservation) {
    return new ReservationResponse(
        reservation.getId(),
        reservation.getOrderId(),
        reservation.getUserId(),
        reservation.getStatus(),
        reservation.getCreatedAt(),
        reservation.getUpdatedAt());
  }

  public record ReserveRequest(
      @Schema(description = "Order ID", example = "1") @NotNull Long orderId,
      @Schema(description = "User ID", example = "1") @NotNull Long userId,
      @Schema(description = "Reservation status", example = "RESERVED") @NotBlank String status) {}

  public record UpdateReservationStatusRequest(
      @Schema(description = "Reservation status", example = "CONFIRMED") @NotBlank String status) {}

  public record ReservationResponse(
      @Schema(description = "Reservation ID", example = "1") Long id,
      @Schema(description = "Order ID", example = "1") Long orderId,
      @Schema(description = "User ID", example = "1") Long userId,
      @Schema(description = "Reservation status", example = "RESERVED") String status,
      @Schema(description = "Created time") LocalDateTime createdAt,
      @Schema(description = "Updated time") LocalDateTime updatedAt) {}

  public record ConsumeMessageRequest(
      @Schema(description = "Message ID", example = "msg-20260423-001") @NotBlank String messageId,
      @Schema(description = "Consumer name", example = "inventory-listener") @NotBlank String consumerName,
      @Schema(description = "Event type", example = "order.created") @NotBlank String eventType) {}

  public record MessageConsumeResponse(
      @Schema(description = "Message ID", example = "msg-20260423-001") String messageId,
      @Schema(description = "Handle result", example = "RECORDED") String result) {}
}
