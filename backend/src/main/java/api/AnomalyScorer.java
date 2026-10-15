package com.threatguard.api;

import org.springframework.stereotype.Service;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnomalyScorer {
  private final WekaAnomalyService weka;
  AnomalyScorer(WekaAnomalyService weka) { this.weka = weka; }
  public void score(SecurityEvent event) {
    int score = 8; List<String> reasons = new ArrayList<>();
    if (event.getEventType().contains("FAILED")) { score += 38; reasons.add("failed authentication"); }
    if (event.getUsername().equalsIgnoreCase("root") || event.getUsername().equalsIgnoreCase("admin")) { score += 18; reasons.add("privileged account"); }
    if (event.getSourceIp().startsWith("185.") || event.getSourceIp().startsWith("45.")) { score += 22; reasons.add("untrusted source range"); }
    if (event.getBytesTransferred() > 5_000_000) { score += 25; reasons.add("unusual data volume"); }
    var now = LocalTime.now(); if (now.isBefore(LocalTime.of(6,0)) || now.isAfter(LocalTime.of(22,0))) { score += 10; reasons.add("off-hours activity"); }
    double mlScore = weka.score(event);
    score = (int) Math.min(100, Math.max(score, mlScore)); String severity = score >= 75 ? "CRITICAL" : score >= 50 ? "HIGH" : score >= 25 ? "MEDIUM" : "LOW";
    String mlExplanation = weka.explanation(event);
    event.score(score, severity, reasons.isEmpty() ? mlExplanation : String.join(", ", reasons) + "; " + mlExplanation);
  }
}
