package com.example.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "consumed_messages")
public class ConsumedMessageEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "message_id", nullable = false, length = 64)
  private String messageId;
  @Column(name = "consumer_name", nullable = false, length = 64)
  private String consumerName;
  @Column(name = "event_type", nullable = false, length = 64)
  private String eventType;
  @Column(name = "consumed_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime consumedAt;

  public Long getId() { return id; }
  public String getMessageId() { return messageId; }
  public void setMessageId(String messageId) { this.messageId = messageId; }
  public String getConsumerName() { return consumerName; }
  public void setConsumerName(String consumerName) { this.consumerName = consumerName; }
  public String getEventType() { return eventType; }
  public void setEventType(String eventType) { this.eventType = eventType; }
  public LocalDateTime getConsumedAt() { return consumedAt; }
}
