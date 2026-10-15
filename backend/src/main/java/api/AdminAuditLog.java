package com.threatguard.api;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="admin_audit_logs")
public class AdminAuditLog {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(nullable=false) private String actor; @Column(nullable=false) private String action;
  @Column(nullable=false) private String target; @Column(length=4000) private String details;
  @Column(nullable=false) private Instant createdAt=Instant.now();
  protected AdminAuditLog(){} public AdminAuditLog(String actor,String action,String target,String details){
    this.actor=actor;this.action=action;this.target=target;this.details=details;
  }
}
