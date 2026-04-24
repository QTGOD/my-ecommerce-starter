package com.example.auth.repository;

import com.example.auth.pojo.RefreshToken;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  List<RefreshToken> findByRevokedAtIsNullOrderByCreatedAtDesc();
}
