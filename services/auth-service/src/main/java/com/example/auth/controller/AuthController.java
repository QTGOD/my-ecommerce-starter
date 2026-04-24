package com.example.auth.controller;

import com.example.auth.pojo.User;
import com.example.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api")
@Tag(name = "Auth", description = "User registration, login and user management APIs")
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/auth/register")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Register user", description = "Create a new user and issue an access token plus refresh token")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Register success"),
      @ApiResponse(responseCode = "400", description = "Request parameters invalid"),
      @ApiResponse(responseCode = "409", description = "Username or email already exists")
  })
  public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
    return toAuthResponse(authService.register(request.username(), request.email(), request.password()));
  }

  @PostMapping("/auth/login")
  @Operation(summary = "Login", description = "Login with email and password and issue a new token pair")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Login success"),
      @ApiResponse(responseCode = "400", description = "Request parameters invalid"),
      @ApiResponse(responseCode = "401", description = "Email or password incorrect")
  })
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    return toAuthResponse(authService.login(request.email(), request.password()));
  }

  @PostMapping("/auth/refresh")
  @Operation(summary = "Refresh token", description = "Use refresh token to rotate token pair")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Refresh success"),
      @ApiResponse(responseCode = "401", description = "Refresh token invalid or expired")
  })
  public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return toAuthResponse(authService.refreshTokens(request.refreshToken()));
  }

  @GetMapping("/users")
  @Operation(summary = "List users", description = "Query users and optionally filter by status")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Query success"),
      @ApiResponse(responseCode = "400", description = "Status invalid")
  })
  public List<UserSummaryResponse> listUsers(@RequestParam(required = false) Integer status) {
    return authService.listUsers(status).stream()
        .map(this::toUserSummaryResponse)
        .toList();
  }

  @GetMapping("/users/{id}")
  @Operation(summary = "Get user", description = "Query basic user information by user ID")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Query success"),
      @ApiResponse(responseCode = "404", description = "User not found")
  })
  public UserSummaryResponse getUser(@PathVariable Long id) {
    return toUserSummaryResponse(authService.getUser(id));
  }

  @PatchMapping("/users/{id}/status")
  @Operation(summary = "Update user status", description = "Change user status, 0 for active and 1 for disabled")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Update success"),
      @ApiResponse(responseCode = "400", description = "Status invalid"),
      @ApiResponse(responseCode = "404", description = "User not found")
  })
  public UserSummaryResponse updateStatus(
      @PathVariable Long id,
      @Valid @RequestBody UpdateUserStatusRequest request) {
    return toUserSummaryResponse(authService.updateStatus(id, request.status()));
  }

  private AuthResponse toAuthResponse(AuthService.AuthTokens tokens) {
    return new AuthResponse(
        tokens.user().getId(),
        tokens.user().getUsername(),
        tokens.user().getStatus(),
        tokens.accessToken(),
        tokens.refreshToken(),
        tokens.refreshTokenExpiresAt());
  }

  private UserSummaryResponse toUserSummaryResponse(User user) {
    return new UserSummaryResponse(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getStatus(),
        user.getCreatedAt(),
        user.getUpdatedAt());
  }

  public record RegisterRequest(
      @Schema(description = "Username", example = "alice")
      @NotBlank String username,
      @Schema(description = "Email", example = "alice@example.com")
      @Email @NotBlank String email,
      @Schema(description = "Password", example = "P@ssw0rd123")
      @NotBlank String password) {}

  public record LoginRequest(
      @Schema(description = "Email", example = "alice@example.com")
      @Email @NotBlank String email,
      @Schema(description = "Password", example = "P@ssw0rd123")
      @NotBlank String password) {}

  public record RefreshTokenRequest(
      @Schema(description = "Refresh token", example = "refresh-7de4c6a0-668e-463e-b65f-523b7b6d24de")
      @NotBlank String refreshToken) {}

  public record UpdateUserStatusRequest(
      @Schema(description = "User status, 0 active, 1 disabled", example = "1")
      @NotNull Integer status) {}

  public record AuthResponse(
      @Schema(description = "User ID", example = "1") Long userId,
      @Schema(description = "Username", example = "alice") String username,
      @Schema(description = "User status", example = "0") Integer status,
      @Schema(description = "Access token", example = "access-2b7aa4ce-6c2d-4fda-bc5a-7b6f2f7f8d7b") String accessToken,
      @Schema(description = "Refresh token", example = "refresh-7de4c6a0-668e-463e-b65f-523b7b6d24de") String refreshToken,
      @Schema(description = "Refresh token expiration time", example = "2026-05-01T10:15:30") LocalDateTime refreshTokenExpiresAt) {}

  public record UserSummaryResponse(
      @Schema(description = "User ID", example = "1") Long id,
      @Schema(description = "Username", example = "alice") String username,
      @Schema(description = "Email", example = "alice@example.com") String email,
      @Schema(description = "Status, 0 active, 1 disabled", example = "0") Integer status,
      @Schema(description = "Created time", example = "2026-04-24T10:15:30") LocalDateTime createdAt,
      @Schema(description = "Updated time", example = "2026-04-24T10:15:30") LocalDateTime updatedAt) {}
}
