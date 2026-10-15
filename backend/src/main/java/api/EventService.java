package com.threatguard.api;

import org.springframework.stereotype.Service;

@Service
public class EventService {
  private final EventRepository events;
  private final AnomalyScorer scorer;
  EventService(EventRepository events, AnomalyScorer scorer) { this.events = events; this.scorer = scorer; }

  public SecurityEvent ingest(String sourceIp, String username, String eventType, long bytes, String customerId, String assetName) {
    SecurityEvent event = new SecurityEvent(sourceIp, username, eventType, bytes, customerId, assetName);
    scorer.score(event);
    return events.save(event);
  }
}
