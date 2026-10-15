package com.threatguard.api;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:8000", "http://127.0.0.1:8000"})
@RequestMapping("/api/demo")
public class DemoAttackController {
  private final RequestLogRepository logs; private final TelemetryFeatureAggregator aggregator;
  private final WekaAnomalyService weka; private final SecurityAlertRepository alerts; private final BlockedIpRepository blocked;
  public DemoAttackController(RequestLogRepository logs,TelemetryFeatureAggregator aggregator,WekaAnomalyService weka,
                              SecurityAlertRepository alerts,BlockedIpRepository blocked){
    this.logs=logs;this.aggregator=aggregator;this.weka=weka;this.alerts=alerts;this.blocked=blocked;
  }
  @PostMapping("/simulate-attack")
  public Map<String,Object> simulate(@RequestParam String type) {
    String normalized=type.toLowerCase(Locale.ROOT);
    if(!Set.of("bruteforce","scan","dos","sqli").contains(normalized))
      throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"type must be bruteforce, scan, dos, or sqli");
    String ip="198.51.100."+new Random().nextInt(200,250);
    int count=switch(normalized){case "bruteforce"->30;case "scan"->45;case "dos"->20;default->5;};
    for(int i=0;i<count;i++) {
      int status=normalized.equals("bruteforce")?401:normalized.equals("scan")?404:200;
      String endpoint=normalized.equals("scan")?"/api/probe/"+i:"/api/demo";
      int signatures=normalized.equals("sqli")?1:0;
      logs.save(new RequestLog(ip,"demo-user",endpoint,"GET",status,normalized.equals("dos")?5000:50,
          normalized.equals("dos")?900:normalized.equals("sqli")?1200:200,"demo-simulator",signatures));
    }
    WekaAnomalyService.Detection d=weka.detect(aggregator.features(ip));
    if(normalized.equals("sqli")) d=new WekaAnomalyService.Detection("PayloadAttack",Math.max(90,d.riskScore()),d.confidence(),"Demo SQL injection signature (' OR 1=1) detected");
    SecurityAlert alert=alerts.save(new SecurityAlert(ip,d.threatClass(),d.riskScore(),SecurityTelemetryFilter.severity(d.riskScore()),d.reason()));
    if(d.riskScore()>90) blocked.save(new BlockedIp(ip,java.time.Instant.now().plusSeconds(900),false,"Demo critical-risk response","DEMO"));
    return Map.of("type",normalized,"clientIp",ip,"requestsGenerated",count,"alertId",alert.getId(),"riskScore",d.riskScore(),"reason",d.reason());
  }
}
