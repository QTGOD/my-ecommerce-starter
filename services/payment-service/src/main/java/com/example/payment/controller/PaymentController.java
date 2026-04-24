package com.example.payment.controller;

import com.example.common.exception.BusinessException;
import com.example.common.exception.ErrorCode;
import com.example.payment.entity.PaymentOrderEntity;
import com.example.payment.repository.PaymentOrderRepository;
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
@RequestMapping("/api/payments")
@Tag(name = "Payment", description = "Payment create, query, status update, and refund simulation APIs")
public class PaymentController {
  private static final Set<String> ALLOWED_PAYMENT_STATUSES = Set.of("INIT", "SUCCESS", "FAILED", "REFUNDED", "CANCELLED");

  private final PaymentOrderRepository paymentOrderRepository;

  public PaymentController(PaymentOrderRepository paymentOrderRepository) {
    this.paymentOrderRepository = paymentOrderRepository;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  @Operation(summary = "Create payment order", description = "Create one payment order in INIT status")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Payment order created"),
      @ApiResponse(responseCode = "400", description = "Invalid request"),
      @ApiResponse(responseCode = "409", description = "Payment order already exists for this order")
  })
  public PaymentResponse createPayment(@Valid @RequestBody CreatePaymentRequest request) {
    if (paymentOrderRepository.findByOrderId(request.orderId()).isPresent()) {
      throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "Payment order already exists for this order");
    }
    PaymentOrderEntity paymentOrder = new PaymentOrderEntity();
    paymentOrder.setPaymentNo("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    paymentOrder.setOrderId(request.orderId());
    paymentOrder.setUserId(request.userId());
    paymentOrder.setAmount(validateAmount(request.amount()));
    paymentOrder.setPaymentMethod(request.paymentMethod().trim().toUpperCase());
    paymentOrder.setStatus("INIT");
    paymentOrderRepository.save(paymentOrder);
    return toPaymentResponse(paymentOrder);
  }

  @PostMapping("/pay")
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  @Operation(summary = "Pay immediately", description = "Create a payment order if missing and mark it as SUCCESS")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Payment succeeded"),
      @ApiResponse(responseCode = "400", description = "Invalid request")
  })
  public PaymentResponse pay(@Valid @RequestBody PayRequest request) {
    PaymentOrderEntity paymentOrder = paymentOrderRepository.findByOrderId(request.orderId()).orElseGet(PaymentOrderEntity::new);
    if (paymentOrder.getId() == null) {
      paymentOrder.setPaymentNo("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
      paymentOrder.setOrderId(request.orderId());
      paymentOrder.setUserId(request.userId());
      paymentOrder.setAmount(validateAmount(request.amount()));
      paymentOrder.setPaymentMethod(request.paymentMethod().trim().toUpperCase());
    }
    applyPaymentStatus(paymentOrder, "SUCCESS", true);
    paymentOrderRepository.save(paymentOrder);
    return toPaymentResponse(paymentOrder);
  }

  @GetMapping
  @Operation(summary = "List payments", description = "Query payment orders by user and optional order or status")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "Query succeeded")})
  public List<PaymentResponse> listPayments(
      @RequestParam(required = false) Long userId,
      @RequestParam(required = false) Long orderId,
      @RequestParam(required = false) String status) {
    List<PaymentOrderEntity> payments;
    if (orderId != null) {
      PaymentOrderEntity payment = paymentOrderRepository.findByOrderId(orderId)
          .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
      payments = List.of(payment);
    } else if (userId != null && status != null && !status.isBlank()) {
      payments = paymentOrderRepository.findByUserIdAndStatusOrderByIdDesc(userId, normalizeStatus(status));
    } else if (userId != null) {
      payments = paymentOrderRepository.findByUserIdOrderByIdDesc(userId);
    } else if (status != null && !status.isBlank()) {
      payments = paymentOrderRepository.findByStatusOrderByIdDesc(normalizeStatus(status));
    } else {
      payments = paymentOrderRepository.findAll().stream()
          .sorted((left, right) -> right.getId().compareTo(left.getId()))
          .toList();
    }
    return payments.stream().map(this::toPaymentResponse).toList();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get payment detail", description = "Query payment order detail by payment id")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Query succeeded"),
      @ApiResponse(responseCode = "404", description = "Payment order not found")
  })
  public PaymentResponse getById(@PathVariable Long id) {
    return toPaymentResponse(getPaymentOrder(id));
  }

  @GetMapping("/order/{orderId}")
  @Operation(summary = "Get payment by order id", description = "Query payment order by order id")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Query succeeded"),
      @ApiResponse(responseCode = "404", description = "Payment order not found")
  })
  public PaymentResponse getByOrderId(@PathVariable Long orderId) {
    PaymentOrderEntity paymentOrder = paymentOrderRepository.findByOrderId(orderId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    return toPaymentResponse(paymentOrder);
  }

  @PatchMapping("/{id}/status")
  @Transactional
  @Operation(summary = "Update payment status", description = "Update payment order status")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Payment status updated"),
      @ApiResponse(responseCode = "400", description = "Invalid status transition"),
      @ApiResponse(responseCode = "404", description = "Payment order not found")
  })
  public PaymentResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdatePaymentStatusRequest request) {
    PaymentOrderEntity paymentOrder = getPaymentOrder(id);
    String targetStatus = normalizeStatus(request.status());
    validatePaymentTransition(paymentOrder.getStatus(), targetStatus);
    applyPaymentStatus(paymentOrder, targetStatus, request.generateTxnId());
    paymentOrderRepository.save(paymentOrder);
    return toPaymentResponse(paymentOrder);
  }

  @PostMapping("/{id}/refund")
  @Transactional
  @Operation(summary = "Refund payment", description = "Simulate refund for a successful payment")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Refund succeeded"),
      @ApiResponse(responseCode = "400", description = "Current payment state does not allow refund"),
      @ApiResponse(responseCode = "404", description = "Payment order not found")
  })
  public PaymentResponse refund(@PathVariable Long id, @Valid @RequestBody RefundRequest request) {
    PaymentOrderEntity paymentOrder = getPaymentOrder(id);
    if (!"SUCCESS".equals(paymentOrder.getStatus())) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST, "Only successful payments can be refunded");
    }
    paymentOrder.setThirdPartyTxnId("REFUND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    paymentOrder.setStatus("REFUNDED");
    paymentOrder.setPaidAt(LocalDateTime.now());
    paymentOrderRepository.save(paymentOrder);
    return toPaymentResponse(paymentOrder, request.reason());
  }

  private PaymentOrderEntity getPaymentOrder(Long id) {
    return paymentOrderRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
  }

  private BigDecimal validateAmount(BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST, "Payment amount must be greater than 0");
    }
    return amount;
  }

  private void validatePaymentTransition(String currentStatus, String targetStatus) {
    if (!ALLOWED_PAYMENT_STATUSES.contains(targetStatus)) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST, "Unsupported payment status: " + targetStatus);
    }
    if (currentStatus.equals(targetStatus)) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST, "Payment status is already " + targetStatus);
    }
    boolean valid = switch (currentStatus) {
      case "INIT" -> Set.of("SUCCESS", "FAILED", "CANCELLED").contains(targetStatus);
      case "FAILED" -> Set.of("SUCCESS", "CANCELLED").contains(targetStatus);
      case "SUCCESS" -> Set.of("REFUNDED").contains(targetStatus);
      case "CANCELLED", "REFUNDED" -> false;
      default -> false;
    };
    if (!valid) {
      throw new BusinessException(
          ErrorCode.INVALID_REQUEST,
          "Invalid payment status transition from " + currentStatus + " to " + targetStatus);
    }
  }

  private void applyPaymentStatus(PaymentOrderEntity paymentOrder, String targetStatus, boolean generateTxnId) {
    paymentOrder.setStatus(targetStatus);
    if ("SUCCESS".equals(targetStatus)) {
      if (generateTxnId || paymentOrder.getThirdPartyTxnId() == null || paymentOrder.getThirdPartyTxnId().isBlank()) {
        paymentOrder.setThirdPartyTxnId("MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
      }
      paymentOrder.setPaidAt(LocalDateTime.now());
    } else if ("REFUNDED".equals(targetStatus)) {
      paymentOrder.setThirdPartyTxnId("REFUND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
      paymentOrder.setPaidAt(LocalDateTime.now());
    } else if ("FAILED".equals(targetStatus) || "CANCELLED".equals(targetStatus)) {
      paymentOrder.setPaidAt(null);
      if ("CANCELLED".equals(targetStatus)) {
        paymentOrder.setThirdPartyTxnId(null);
      }
    }
  }

  private PaymentResponse toPaymentResponse(PaymentOrderEntity paymentOrder) {
    return toPaymentResponse(paymentOrder, null);
  }

  private PaymentResponse toPaymentResponse(PaymentOrderEntity paymentOrder, String note) {
    return new PaymentResponse(
        paymentOrder.getId(),
        paymentOrder.getPaymentNo(),
        paymentOrder.getOrderId(),
        paymentOrder.getUserId(),
        paymentOrder.getAmount(),
        paymentOrder.getPaymentMethod(),
        paymentOrder.getStatus(),
        paymentOrder.getThirdPartyTxnId(),
        paymentOrder.getCreatedAt(),
        paymentOrder.getPaidAt(),
        paymentOrder.getUpdatedAt(),
        note);
  }

  private String normalizeStatus(String value) {
    return value.trim().toUpperCase();
  }

  public record CreatePaymentRequest(
      @Schema(description = "Order id", example = "1")
      @NotNull Long orderId,
      @Schema(description = "User id", example = "1")
      @NotNull Long userId,
      @Schema(description = "Payment amount", example = "798.00")
      @NotNull BigDecimal amount,
      @Schema(description = "Payment method", example = "MOCK_WECHAT")
      @NotBlank String paymentMethod) {}

  public record PayRequest(
      @Schema(description = "Order id", example = "1")
      @NotNull Long orderId,
      @Schema(description = "User id", example = "1")
      @NotNull Long userId,
      @Schema(description = "Payment amount", example = "798.00")
      @NotNull BigDecimal amount,
      @Schema(description = "Payment method", example = "MOCK_WECHAT")
      @NotBlank String paymentMethod) {}

  public record UpdatePaymentStatusRequest(
      @Schema(description = "Target payment status", example = "SUCCESS")
      @NotBlank String status,
      @Schema(description = "Whether to generate a mock transaction id when payment succeeds", example = "true")
      boolean generateTxnId) {}

  public record RefundRequest(
      @Schema(description = "Refund reason", example = "User requested refund")
      @NotBlank String reason) {}

  public record PaymentResponse(
      @Schema(description = "Payment id", example = "1") Long id,
      @Schema(description = "Payment number", example = "PAY-AB12CD34") String paymentNo,
      @Schema(description = "Order id", example = "1") Long orderId,
      @Schema(description = "User id", example = "1") Long userId,
      @Schema(description = "Payment amount", example = "798.00") BigDecimal amount,
      @Schema(description = "Payment method", example = "MOCK_WECHAT") String paymentMethod,
      @Schema(description = "Payment status", example = "SUCCESS") String status,
      @Schema(description = "Third-party transaction id", example = "MOCK-EF56GH78") String thirdPartyTxnId,
      @Schema(description = "Created time") LocalDateTime createdAt,
      @Schema(description = "Paid time") LocalDateTime paidAt,
      @Schema(description = "Updated time") LocalDateTime updatedAt,
      @Schema(description = "Additional note", example = "User requested refund") String note) {}
}
