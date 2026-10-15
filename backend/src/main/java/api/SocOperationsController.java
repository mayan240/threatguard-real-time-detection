package com.threatguard.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:8000", "http://127.0.0.1:8000"})
@RequestMapping("/api/soc")
public class SocOperationsController {
  private final SecurityAlertRepository alerts; private final BlockedIpRepository blocked; private final AdminAuditLogRepository audit;
  private final RequestLogRepository logs; private final WekaAnomalyService weka;
  public SocOperationsController(SecurityAlertRepository a,BlockedIpRepository b,AdminAuditLogRepository audit,RequestLogRepository logs,WekaAnomalyService w){
    alerts=a;blocked=b;this.audit=audit;this.logs=logs;weka=w;
  }
  @GetMapping("/stats") public Map<String,Object> stats(){return Map.of("openAlerts",alerts.countByStatus("OPEN"),"blockedIps",blocked.count(),"modelSensitivity",weka.getSensitivity());}
  @GetMapping("/alerts") public List<SecurityAlert> alertList(){return alerts.findTop100ByOrderByCreatedAtDesc();}
  @PatchMapping("/alerts/{id}") public SecurityAlert triage(@PathVariable Long id,@RequestBody Triage body,@RequestHeader("X-SOC-Role") String role,@RequestHeader(value="X-SOC-User",defaultValue="unknown") String actor){
    requireViewer(role); if(!Set.of("FALSE_POSITIVE","RESOLVED","OPEN").contains(body.status()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid alert status");
    SecurityAlert a=alerts.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)); a.resolve(actor,body.status()); audit.save(new AdminAuditLog(actor,"ALERT_"+body.status(),String.valueOf(id),"Alert triage")); return alerts.save(a);
  }
  @PostMapping("/block") public BlockedIp block(@Valid @RequestBody Block body,@RequestHeader("X-SOC-Role") String role,@RequestHeader(value="X-SOC-User",defaultValue="unknown") String actor){
    requireAdmin(role); BlockedIp b=new BlockedIp(body.ip(),body.permanent()?null:Instant.now().plusSeconds(Math.max(60,body.minutes()*60L)),body.permanent(),body.reason(),actor);
    audit.save(new AdminAuditLog(actor,"BLOCK_IP",body.ip(),body.reason())); return blocked.save(b);
  }
  @DeleteMapping("/block/{ip}") public Map<String,String> unblock(@PathVariable String ip,@RequestHeader("X-SOC-Role") String role,@RequestHeader(value="X-SOC-User",defaultValue="unknown") String actor){
    requireAdmin(role); BlockedIp b=blocked.findByClientIp(ip).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)); b.unblock();blocked.save(b);audit.save(new AdminAuditLog(actor,"UNBLOCK_IP",ip,"Manual SOC action"));return Map.of("status","unblocked");
  }
  @PutMapping("/sensitivity") public Map<String,Double> sensitivity(@Valid @RequestBody Sensitivity body,@RequestHeader("X-SOC-Role") String role,@RequestHeader(value="X-SOC-User",defaultValue="unknown") String actor){
    requireAdmin(role);weka.setSensitivity(body.value());audit.save(new AdminAuditLog(actor,"CHANGE_SENSITIVITY","model",String.valueOf(body.value())));return Map.of("sensitivity",weka.getSensitivity());
  }
  @GetMapping(value="/export.csv",produces="text/csv") public String export(){return "ip,endpoint,status,response_ms,payload_bytes,signature_hits,timestamp\n"+logs.findAll().stream().map(r->String.join(",",r.getClientIp(),r.getEndpoint(),String.valueOf(r.getStatusCode()),String.valueOf(r.getResponseTimeMs()),String.valueOf(r.getPayloadSizeBytes()),String.valueOf(r.getSignatureHitCount()),r.getCreatedAt().toString())).collect(Collectors.joining("\n"));}
  private void requireViewer(String role){if(!"SOC_ADMIN".equals(role)&&!"SOC_VIEWER".equals(role))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"SOC role required");}
  private void requireAdmin(String role){if(!"SOC_ADMIN".equals(role))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"SOC_ADMIN role required");}
  public record Triage(@NotBlank String status){} public record Sensitivity(@DecimalMin("0") @DecimalMax("100") double value){}
  public record Block(@NotBlank String ip,@NotBlank String reason,@Min(1) int minutes,boolean permanent){}
}
