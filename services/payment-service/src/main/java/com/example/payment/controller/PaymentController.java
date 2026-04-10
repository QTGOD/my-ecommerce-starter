package com.example.payment.controller;

import com.example.common.exception.BusinessException;
import com.example.common.exception.ErrorCode;
import com.example.payment.entity.PaymentOrderEntity;
import com.example.payment.repository.PaymentOrderRepository;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
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
@RequestMapping("/api/payments")
@Tag(name = "Payment", description = "支付创建与查询接口")
public class PaymentController {
  private final PaymentOrderRepository paymentOrderRepository;

  public PaymentController(PaymentOrderRepository paymentOrderRepository) {
    this.paymentOrderRepository = paymentOrderRepository;
  }

  @PostMapping("/pay")
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  @Operation(summary = "模拟支付", description = "为指定订单创建或更新支付单，并将状态置为 SUCCESS")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "支付成功"),
      @ApiResponse(responseCode = "400", description = "请求参数不合法")
  })
  public PaymentResponse pay(@Valid @RequestBody PayRequest request) {
    PaymentOrderEntity paymentOrder = paymentOrderRepository.findByOrderId(request.orderId()).orElseGet(PaymentOrderEntity::new);
    if (paymentOrder.getId() == null) {
      paymentOrder.setPaymentNo("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
      paymentOrder.setOrderId(request.orderId());
      paymentOrder.setUserId(request.userId());
      paymentOrder.setAmount(request.amount());
      paymentOrder.setPaymentMethod(request.paymentMethod());
    }
    paymentOrder.setStatus("SUCCESS");
    paymentOrder.setThirdPartyTxnId("MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    paymentOrder.setPaidAt(LocalDateTime.now());
    paymentOrderRepository.save(paymentOrder);

    return new PaymentResponse(
        paymentOrder.getId(),
        paymentOrder.getPaymentNo(),
        paymentOrder.getOrderId(),
        paymentOrder.getStatus(),
        paymentOrder.getThirdPartyTxnId());
  }

  @GetMapping("/order/{orderId}")
  @Operation(summary = "按订单查询支付单", description = "根据订单 ID 查询对应支付结果")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "查询成功"),
      @ApiResponse(responseCode = "404", description = "支付单不存在")
  })
  public PaymentResponse getByOrderId(@PathVariable Long orderId) {
    PaymentOrderEntity paymentOrder = paymentOrderRepository.findByOrderId(orderId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    return new PaymentResponse(
        paymentOrder.getId(),
        paymentOrder.getPaymentNo(),
        paymentOrder.getOrderId(),
        paymentOrder.getStatus(),
        paymentOrder.getThirdPartyTxnId());
  }

  public record PayRequest(
      @Schema(description = "订单 ID", example = "1")
      @NotNull Long orderId,
      @Schema(description = "用户 ID", example = "1")
      @NotNull Long userId,
      @Schema(description = "支付金额", example = "798.00")
      @NotNull BigDecimal amount,
      @Schema(description = "支付方式", example = "MOCK_WECHAT")
      @NotBlank String paymentMethod) {}

  public record PaymentResponse(
      @Schema(description = "支付主键 ID", example = "1") Long id,
      @Schema(description = "支付单号", example = "PAY-AB12CD34") String paymentNo,
      @Schema(description = "订单 ID", example = "1") Long orderId,
      @Schema(description = "支付状态", example = "SUCCESS") String status,
      @Schema(description = "第三方交易流水号", example = "MOCK-EF56GH78") String thirdPartyTxnId) {}
}
