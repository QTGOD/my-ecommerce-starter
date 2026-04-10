package com.example.auth.controller;

import com.example.auth.pojo.RefreshToken;
import com.example.auth.repository.RefreshTokenRepository;
import com.example.auth.pojo.User;
import com.example.auth.repository.UserRepository;
import com.example.common.exception.BusinessException;
import com.example.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Auth", description = "用户注册、登录与基础用户查询接口")
public class AuthController {
  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  public AuthController(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
  }

  @PostMapping("/auth/register")
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  @Operation(summary = "注册用户", description = "创建新用户并签发一组模拟 accessToken / refreshToken")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "注册成功"),
      @ApiResponse(responseCode = "400", description = "请求参数不合法"),
      @ApiResponse(responseCode = "409", description = "用户名或邮箱已存在")
  })
  public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
    if (userRepository.existsByUsername(request.username()) || userRepository.existsByEmail(request.email())) {
      throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
    }

    User user = new User();
    user.setUsername(request.username());
    user.setEmail(request.email());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setStatus(0);
    userRepository.save(user);

    return issueTokens(user);
  }

  @PostMapping("/auth/login")
  @Transactional
  @Operation(summary = "用户登录", description = "使用邮箱和密码登录，并返回一组模拟 token")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "登录成功"),
      @ApiResponse(responseCode = "400", description = "请求参数不合法"),
      @ApiResponse(responseCode = "401", description = "邮箱或密码错误")
  })
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    User user = userRepository.findByEmail(request.email())
        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
    }

    return issueTokens(user);
  }

  @GetMapping("/users/{id}")
  @Operation(summary = "查询用户", description = "根据用户 ID 查询基础用户信息")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "查询成功"),
      @ApiResponse(responseCode = "404", description = "用户不存在")
  })
  public Map<String, Object> getUser(@PathVariable Long id) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    return Map.of(
        "id", user.getId(),
        "username", user.getUsername(),
        "email", user.getEmail(),
        "status", user.getStatus());
  }

  private AuthResponse issueTokens(User user) {
    String accessToken = "access-" + UUID.randomUUID();
    String refreshTokenRaw = "refresh-" + UUID.randomUUID();

    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setUserId(user.getId());
    refreshToken.setTokenHash(passwordEncoder.encode(refreshTokenRaw));
    refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
    refreshTokenRepository.save(refreshToken);

    return new AuthResponse(user.getId(), user.getUsername(), accessToken, refreshTokenRaw);
  }

  public record RegisterRequest(
      @Schema(description = "用户名", example = "alice")
      @NotBlank String username,
      @Schema(description = "邮箱", example = "alice@example.com")
      @Email @NotBlank String email,
      @Schema(description = "密码", example = "P@ssw0rd123")
      @NotBlank String password) {}

  public record LoginRequest(
      @Schema(description = "邮箱", example = "alice@example.com")
      @Email @NotBlank String email,
      @Schema(description = "密码", example = "P@ssw0rd123")
      @NotBlank String password) {}

  public record AuthResponse(
      @Schema(description = "用户 ID", example = "1") Long userId,
      @Schema(description = "用户名", example = "alice") String username,
      @Schema(description = "访问令牌", example = "access-2b7aa4ce-6c2d-4fda-bc5a-7b6f2f7f8d7b") String accessToken,
      @Schema(description = "刷新令牌", example = "refresh-7de4c6a0-668e-463e-b65f-523b7b6d24de") String refreshToken) {}
}
