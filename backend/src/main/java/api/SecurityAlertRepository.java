package com.threatguard.api;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SecurityAlertRepository extends JpaRepository<SecurityAlert,Long> { List<SecurityAlert> findTop100ByOrderByCreatedAtDesc(); long countByStatus(String status); }
