package com.threatguard.api;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface BlockedIpRepository extends JpaRepository<BlockedIp,Long> { Optional<BlockedIp> findByClientIp(String clientIp); }
