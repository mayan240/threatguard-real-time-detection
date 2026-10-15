package com.threatguard.api;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "request_logs", indexes = @Index(name = "idx_request_logs_ip_time", columnList = "clientIp,createdAt"))
public class RequestLog {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(nullable = false) private String clientIp;
  private String userId;
  @Column(nullable = false, length = 1000) private String endpoint;
  @Column(nullable = false, length = 16) private String httpMethod;
  @Column(nullable = false) private int statusCode;
  @Column(nullable = false) private long responseTimeMs;
  @Column(nullable = false) private long payloadSizeBytes;
  @Column(length = 1000) private String userAgent;
  @Column(nullable = false) private int signatureHitCount;
  @Column(nullable = false) private Instant createdAt;

  protected RequestLog() {}
  public RequestLog(String ip, String userId, String endpoint, String method, int status, long time,
                    long payload, String agent, int signatures) {
    clientIp = ip; this.userId = userId; this.endpoint = endpoint; httpMethod = method;
    statusCode = status; responseTimeMs = time; payloadSizeBytes = payload; userAgent = agent;
    signatureHitCount = signatures; createdAt = Instant.now();
  }
  public String getClientIp() { return clientIp; }
  public String getEndpoint() { return endpoint; }
  public int getStatusCode() { return statusCode; }
  public long getResponseTimeMs() { return responseTimeMs; }
  public long getPayloadSizeBytes() { return payloadSizeBytes; }
  public int getSignatureHitCount() { return signatureHitCount; }
  public Instant getCreatedAt() { return createdAt; }
}
