package com.example.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiErrorResponse> handleBusinessException(
      BusinessException ex,
      HttpServletRequest request) {
    ErrorCode errorCode = ex.getErrorCode();
    return buildResponse(
        errorCode.getHttpStatus(),
        errorCode.getCode(),
        ex.getMessage(),
        request.getRequestURI());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException ex,
      HttpServletRequest request) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + " " + error.getDefaultMessage())
        .collect(Collectors.joining("; "));
    return buildResponse(HttpStatus.BAD_REQUEST, "COMMON_400_001", message, request.getRequestURI());
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(
      ConstraintViolationException ex,
      HttpServletRequest request) {
    return buildResponse(HttpStatus.BAD_REQUEST, "COMMON_400_002", ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleException(
      Exception ex,
      HttpServletRequest request) {
    return buildResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "COMMON_500_001",
        "Internal server error",
        request.getRequestURI());
  }

  private ResponseEntity<ApiErrorResponse> buildResponse(
      HttpStatus status,
      String code,
      String message,
      String path) {
    return ResponseEntity.status(status).body(
        new ApiErrorResponse(code, message, status.value(), path, OffsetDateTime.now()));
  }
}
