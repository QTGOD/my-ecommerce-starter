package com.example.auth.repository;

import java.util.Optional;

import com.example.auth.pojo.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  boolean existsByUsername(String username);
  boolean existsByEmail(String email);
  Optional<User> findByEmail(String email);
}
