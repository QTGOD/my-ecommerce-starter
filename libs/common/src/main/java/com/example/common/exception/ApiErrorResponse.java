package com.example.common.exception;

import java.time.OffsetDateTime;

public record ApiErrorResponse(
    String code,
    String message,
    int status,
    String path,
    OffsetDateTime timestamp) {}
