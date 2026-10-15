package com.threatguard.api;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "blocked_ips")
public class BlockedIp {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(nullable = false, unique = true) private String clientIp;
  private Instant blockedUntil;
  @Column(nullable = false) private boolean permanent;
  private String reason;
  private String createdBy;
  @Column(nullable = false) private Instant createdAt;
  protected BlockedIp() {}
  public BlockedIp(String ip, Instant until, boolean permanent, String reason, String actor) {
    clientIp=ip; blockedUntil=until; this.permanent=permanent; this.reason=reason;
    createdBy=actor; createdAt=Instant.now();
  }
  public String getClientIp(){return clientIp;} public Instant getBlockedUntil(){return blockedUntil;}
  public boolean isPermanent(){return permanent;}
  public boolean active(Instant now){return permanent || (blockedUntil != null && blockedUntil.isAfter(now));}
  public void unblock(){permanent=false; blockedUntil=Instant.EPOCH;}
}
