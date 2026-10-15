package com.threatguard.api;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class SecurityEvent {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  private Instant observedAt = Instant.now();
  private String sourceIp;
  private String username;
  private String eventType;
  private String customerId;
  private String assetName;
  private long bytesTransferred;
  private double anomalyScore;
  private String severity;
  private String rationale;
  private String alertStatus = "OPEN";
  protected SecurityEvent() {}
  public SecurityEvent(String sourceIp, String username, String eventType, long bytesTransferred, String customerId, String assetName) {
    this.sourceIp=sourceIp; this.username=username; this.eventType=eventType; this.bytesTransferred=bytesTransferred; this.customerId=customerId; this.assetName=assetName;
  }
  public Long getId(){return id;} public Instant getObservedAt(){return observedAt;} public String getSourceIp(){return sourceIp;}
  public String getUsername(){return username;} public String getEventType(){return eventType;} public long getBytesTransferred(){return bytesTransferred;}
  public double getAnomalyScore(){return anomalyScore;} public String getSeverity(){return severity;} public String getRationale(){return rationale;}
  public String getCustomerId(){return customerId;} public String getAssetName(){return assetName;} public String getAlertStatus(){return alertStatus;}
  public void score(double score, String severity, String rationale){this.anomalyScore=score;this.severity=severity;this.rationale=rationale;}
  public void triage(String status){this.alertStatus=status;}
}
