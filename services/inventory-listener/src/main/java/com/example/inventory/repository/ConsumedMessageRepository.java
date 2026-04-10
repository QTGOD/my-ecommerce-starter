package com.example.inventory.repository;

import com.example.inventory.entity.ConsumedMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsumedMessageRepository extends JpaRepository<ConsumedMessageEntity, Long> {
  boolean existsByMessageIdAndConsumerName(String messageId, String consumerName);
}
