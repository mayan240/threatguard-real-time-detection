package com.threatguard.api;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EventRepository extends JpaRepository<SecurityEvent, Long> { List<SecurityEvent> findTop50ByOrderByObservedAtDesc(); long countBySeverityIn(List<String> severities); long countByAlertStatus(String status); }
