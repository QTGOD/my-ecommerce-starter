package com.example.auth.service;

import com.example.auth.pojo.RefreshToken;
import com.example.auth.pojo.User;
import com.example.auth.repository.RefreshTokenRepository;
import com.example.auth.repository.UserRepository;
import com.example.common.exception.BusinessException;
import com.example.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  public static final int STATUS_ACTIVE = 0;
  public static final int STATUS_DISABLED = 1;

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
  }

  @Transactional
  public AuthTokens register(String username, String email, String password) {
    if (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) {
      throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
    }

    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(password));
    user.setStatus(STATUS_ACTIVE);
    userRepository.save(user);

    return issueTokens(user);
  }

  @Transactional
  public AuthTokens login(String email, String password) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
    }
    ensureUserEnabled(user);

    return issueTokens(user);
  }

  @Transactional(readOnly = true)
  public User getUser(Long id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
  }

  @Transactional(readOnly = true)
  public List<User> listUsers(Integer status) {
    if (status == null) {
      return userRepository.findAll().stream()
          .sorted(Comparator.comparing(User::getId))
          .toList();
    }
    validateStatus(status);
    return userRepository.findByStatusOrderByIdAsc(status);
  }

  @Transactional
  public User updateStatus(Long userId, Integer status) {
    validateStatus(status);
    User user = getUser(userId);
    user.setStatus(status);
    return userRepository.save(user);
  }

  @Transactional
  public AuthTokens refreshTokens(String refreshTokenRaw) {
    LocalDateTime now = LocalDateTime.now();
    RefreshToken matchedToken = refreshTokenRepository.findByRevokedAtIsNullOrderByCreatedAtDesc().stream()
        .filter(token -> token.getExpiresAt().isAfter(now))
        .filter(token -> passwordEncoder.matches(refreshTokenRaw, token.getTokenHash()))
        .findFirst()
        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Refresh token is invalid or expired"));

    User user = getUser(matchedToken.getUserId());
    ensureUserEnabled(user);

    matchedToken.setRevokedAt(now);
    refreshTokenRepository.save(matchedToken);

    return issueTokens(user);
  }

  private void validateStatus(Integer status) {
    if (status == null || (status != STATUS_ACTIVE && status != STATUS_DISABLED)) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST, "Status must be 0 (active) or 1 (disabled)");
    }
  }

  private void ensureUserEnabled(User user) {
    if (user.getStatus() == null || user.getStatus() != STATUS_ACTIVE) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST, "User is disabled");
    }
  }

  private AuthTokens issueTokens(User user) {
    String accessToken = "access-" + UUID.randomUUID();
    String refreshTokenRaw = "refresh-" + UUID.randomUUID();

    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setUserId(user.getId());
    refreshToken.setTokenHash(passwordEncoder.encode(refreshTokenRaw));
    refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
    refreshTokenRepository.save(refreshToken);

    return new AuthTokens(user, accessToken, refreshTokenRaw, refreshToken.getExpiresAt());
  }

  public record AuthTokens(User user, String accessToken, String refreshToken, LocalDateTime refreshTokenExpiresAt) {}
}
