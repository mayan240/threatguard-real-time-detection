package com.threatguard.api;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;

public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {
  List<RequestLog> findByClientIpAndCreatedAtAfter(String clientIp, Instant since);
  @Query("select r from RequestLog r where r.clientIp = :ip and r.createdAt > :since")
  List<RequestLog> window(@Param("ip") String ip, @Param("since") Instant since);
}
