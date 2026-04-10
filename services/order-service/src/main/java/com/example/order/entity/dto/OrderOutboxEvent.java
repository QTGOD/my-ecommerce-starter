package com.example.order.entity.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_outbox_events")
public class OrderOutboxEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "aggregate_type", nullable = false, length = 32)
  private String aggregateType;
  @Column(name = "aggregate_id", nullable = false)
  private Long aggregateId;
  @Column(name = "event_type", nullable = false, length = 64)
  private String eventType;
  @Column(name = "payload_json", nullable = false, columnDefinition = "json")
  private String payloadJson;
  @Column(nullable = false, length = 32)
  private String status = "NEW";
  @Column(name = "retry_count", nullable = false)
  private Integer retryCount = 0;
  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime createdAt;
  @Column(name = "sent_at")
  private LocalDateTime sentAt;

  public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }
  public void setAggregateId(Long aggregateId) { this.aggregateId = aggregateId; }
  public void setEventType(String eventType) { this.eventType = eventType; }
  public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
  public void setStatus(String status) { this.status = status; }
}
