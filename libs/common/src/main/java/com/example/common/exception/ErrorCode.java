package com.example.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400_003", "Invalid request"),
  RESOURCE_CONFLICT(HttpStatus.CONFLICT, "COMMON_409_001", "Resource conflict"),
  USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH_409_001", "Username or email already exists"),
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_401_001", "Invalid credentials"),
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_404_001", "User not found"),
  PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_404_001", "Product not found"),
  PRODUCT_SLUG_EXISTS(HttpStatus.CONFLICT, "PRODUCT_409_001", "Product slug already exists"),
  ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_404_001", "Order not found"),
  EVENT_SERIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ORDER_500_001", "Failed to serialize event payload"),
  PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_404_001", "Payment not found"),
  INVENTORY_RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "INVENTORY_404_001", "Reservation not found");

  private final HttpStatus httpStatus;
  private final String code;
  private final String defaultMessage;

  ErrorCode(HttpStatus httpStatus, String code, String defaultMessage) {
    this.httpStatus = httpStatus;
    this.code = code;
    this.defaultMessage = defaultMessage;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  public String getCode() {
    return code;
  }

  public String getDefaultMessage() {
    return defaultMessage;
  }
}
