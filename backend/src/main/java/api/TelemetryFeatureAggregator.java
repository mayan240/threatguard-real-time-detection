package com.threatguard.api;

import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class TelemetryFeatureAggregator {
  private final RequestLogRepository logs;
  public TelemetryFeatureAggregator(RequestLogRepository logs){this.logs=logs;}
  public double[] features(String ip) {
    List<RequestLog> rows=logs.findByClientIpAndCreatedAtAfter(ip,Instant.now().minusSeconds(60));
    int n=rows.size(); if(n==0)return new double[8];
    long errors=rows.stream().filter(r->r.getStatusCode()>=400).count();
    return new double[]{n, rows.stream().filter(r->r.getStatusCode()==401||r.getStatusCode()==403).count(),
      rows.stream().filter(r->r.getStatusCode()==404).count(), rows.stream().map(RequestLog::getEndpoint).distinct().count(),
      (double)errors/n, rows.stream().mapToLong(RequestLog::getResponseTimeMs).average().orElse(0),
      rows.stream().mapToLong(RequestLog::getPayloadSizeBytes).average().orElse(0),
      rows.stream().mapToInt(RequestLog::getSignatureHitCount).sum()};
  }
}
