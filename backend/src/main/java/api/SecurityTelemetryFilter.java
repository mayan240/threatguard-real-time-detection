package com.threatguard.api;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class SecurityTelemetryFilter extends OncePerRequestFilter {
  private static final List<Pattern> SIGNATURES = List.of(
      Pattern.compile("(?i)(union\\s+select|'\\s*or\\s+1\\s*=\\s*1|--\\s*$)"),
      Pattern.compile("(?i)(<script|javascript:|onerror\\s*=)"),
      Pattern.compile("(?i)(\\.\\./|/\\.env|/etc/passwd)"));
  private final BlockedIpRepository blocked; private final RequestLogRepository logs;
  private final SecurityAlertRepository alerts; private final TelemetryFeatureAggregator features; private final WekaAnomalyService weka;
  public SecurityTelemetryFilter(BlockedIpRepository blocked,RequestLogRepository logs,SecurityAlertRepository alerts,
                                  TelemetryFeatureAggregator features,WekaAnomalyService weka){
    this.blocked=blocked;this.logs=logs;this.alerts=alerts;this.features=features;this.weka=weka;
  }
  @Override protected void doFilterInternal(HttpServletRequest raw,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
    String ip=clientIp(raw);
    if(blocked.findByClientIp(ip).filter(b->b.active(Instant.now())).isPresent()){response.sendError(403,"Client IP is blocked");return;}
    ContentCachingRequestWrapper request=new ContentCachingRequestWrapper(raw);
    int hits=signatureHits(request); long start=System.nanoTime();
    try { chain.doFilter(request,response); }
    finally {
      long elapsed=(System.nanoTime()-start)/1_000_000;
      RequestLog log=logs.save(new RequestLog(ip,request.getHeader("X-User-Id"),request.getRequestURI(),request.getMethod(),
          response.getStatus(),elapsed,request.getContentAsByteArray().length,request.getHeader("User-Agent"),hits));
      if(hits>0 || response.getStatus()==401 || response.getStatus()==403) {
        WekaAnomalyService.Detection d=weka.detect(features.features(ip));
        if(hits>0)d=new WekaAnomalyService.Detection("PayloadAttack",Math.max(85,d.riskScore()),d.confidence(),
            "SQLi/XSS/path traversal signature detected before controller execution; "+d.reason());
        if(weka.shouldAlert(d.riskScore())) alerts.save(new SecurityAlert(ip,d.threatClass(),d.riskScore(),severity(d.riskScore()),d.reason()));
        if(d.riskScore()>90 && blocked.findByClientIp(ip).isEmpty())
          blocked.save(new BlockedIp(ip,Instant.now().plusSeconds(900),false,"Automatic critical-risk response","SYSTEM"));
      }
    }
  }
  private int signatureHits(HttpServletRequest req){
    int hits=0; StringBuilder data=new StringBuilder();
    req.getParameterMap().forEach((name, values) -> {
      data.append(' ').append(name);
      for (String value : values) data.append(' ').append(value);
    });
    Collections.list(req.getHeaderNames()).forEach(n->data.append(' ').append(n).append(' ').append(req.getHeader(n)));
    for(Pattern p:SIGNATURES) if(p.matcher(data).find())hits++; return hits;
  }
  private String clientIp(HttpServletRequest r){String forwarded=r.getHeader("X-Forwarded-For");return forwarded==null?r.getRemoteAddr():forwarded.split(",")[0].trim();}
  public static String severity(int score){return score>=85?"CRITICAL":score>=61?"HIGH":score>=31?"LOW":"NORMAL";}
}
