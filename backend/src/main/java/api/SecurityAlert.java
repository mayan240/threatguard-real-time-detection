package com.threatguard.api;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="security_alerts")
public class SecurityAlert {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(nullable=false) private String clientIp;
  @Column(nullable=false) private String threatClass;
  @Column(nullable=false) private int riskScore;
  @Column(nullable=false) private String severity;
  @Column(nullable=false,length=4000) private String reason;
  @Column(nullable=false) private String status="OPEN";
  @Column(nullable=false) private Instant createdAt=Instant.now();
  private Instant resolvedAt; private String resolvedBy;
  protected SecurityAlert(){}
  public SecurityAlert(String ip,String type,int score,String severity,String reason){
    clientIp=ip; threatClass=type; riskScore=score; this.severity=severity; this.reason=reason;
  }
  public Long getId(){return id;} public String getClientIp(){return clientIp;} public int getRiskScore(){return riskScore;}
  public String getThreatClass(){return threatClass;} public String getSeverity(){return severity;}
  public String getReason(){return reason;} public String getStatus(){return status;} public Instant getCreatedAt(){return createdAt;}
  public void resolve(String actor,String status){this.status=status;resolvedBy=actor;resolvedAt=Instant.now();}
}
