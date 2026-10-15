package com.threatguard.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:8000", "http://127.0.0.1:8000"})
public class ThreatController {
  private final EventRepository events; private final EventService eventService; private final WekaAnomalyService weka; private final WebsiteHealthService websiteHealth;
  ThreatController(EventRepository events, EventService eventService, WekaAnomalyService weka, WebsiteHealthService websiteHealth){this.events=events;this.eventService=eventService;this.weka=weka;this.websiteHealth=websiteHealth;}
  @GetMapping("/events") public List<SecurityEvent> recent(){ return events.findTop50ByOrderByObservedAtDesc(); }
  @GetMapping("/alerts") public List<SecurityEvent> alerts(){ return recent().stream().filter(e -> e.getAnomalyScore() >= 50).toList(); }
  @GetMapping("/metrics") public Map<String,Object> metrics(){
    List<SecurityEvent> all=events.findAll(); double average=all.stream().mapToDouble(SecurityEvent::getAnomalyScore).average().orElse(0);
    return Map.of("events",all.size(),"critical",events.countBySeverityIn(List.of("CRITICAL")),"high",events.countBySeverityIn(List.of("HIGH")),"openAlerts",events.countByAlertStatus("OPEN"),"averageRisk",Math.round(average*10)/10.0);
  }
  @GetMapping("/ml/metrics") public ModelMetrics modelMetrics(){ return weka.metrics(); }
  @PostMapping("/demo/scenarios/{scenario}") public ScenarioResponse runScenario(@PathVariable String scenario){
    List<IngestRequest> requests = switch (scenario.toLowerCase(Locale.ROOT)) {
      case "brute-force" -> List.of(
          new IngestRequest("185.220.101.42","admin","FAILED_LOGIN",500,"demo-workspace","auth-gateway"),
          new IngestRequest("185.220.101.42","admin","FAILED_LOGIN",500,"demo-workspace","auth-gateway"),
          new IngestRequest("185.220.101.42","admin","ACCOUNT_LOCKOUT",500,"demo-workspace","auth-gateway"));
      case "data-exfiltration" -> List.of(
          new IngestRequest("45.142.214.19","analyst","FILE_ACCESS",18_000_000,"demo-workspace","customer-db"),
          new IngestRequest("45.142.214.19","analyst","FILE_EXPORT",24_000_000,"demo-workspace","customer-db"));
      case "privilege-abuse" -> List.of(
          new IngestRequest("10.0.0.24","root","PRIVILEGE_CHANGE",2_000,"demo-workspace","payments-api"),
          new IngestRequest("10.0.0.24","root","FILE_ACCESS",8_000_000,"demo-workspace","customer-db"));
      case "normal-activity" -> List.of(
          new IngestRequest("10.0.0.24","analyst","LOGIN_SUCCESS",1_000,"demo-workspace","auth-gateway"),
          new IngestRequest("10.0.0.24","analyst","API_REQUEST",12_000,"demo-workspace","payments-api"));
      default -> throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Scenario must be brute-force, data-exfiltration, privilege-abuse, or normal-activity");
    };
    List<SecurityEvent> created = requests.stream().map(this::ingestEvent).toList();
    return new ScenarioResponse(scenario, created.size(), created);
  }
  @PostMapping("/website/check") public WebsiteHealthService.WebsiteHealth checkWebsite(@Valid @RequestBody WebsiteCheckRequest request){
    return websiteHealth.check(request.url());
  }
  @PostMapping("/events") @ResponseStatus(HttpStatus.CREATED) public SecurityEvent ingest(@Valid @RequestBody IngestRequest request, @RequestHeader(value="X-API-Key", required=false) String apiKey){
    requireIngestKey(apiKey);
    return ingestEvent(request);
  }
  @PostMapping("/integration/events") public BatchIngestResponse ingestBatch(@Valid @RequestBody BatchIngestRequest request, @RequestHeader(value="X-API-Key", required=false) String apiKey){
    requireIngestKey(apiKey);
    List<SecurityEvent> ingested=request.events().stream().map(this::ingestEvent).toList();
    return new BatchIngestResponse(ingested.size(),ingested);
  }
  private SecurityEvent ingestEvent(IngestRequest request){
    return eventService.ingest(request.sourceIp(),request.username(),request.eventType(),request.bytesTransferred(),request.customerId()==null?"demo-workspace":request.customerId(),request.assetName()==null?"unassigned":request.assetName());
  }
  private void requireIngestKey(String apiKey){
    String requiredKey=System.getenv().getOrDefault("THREATGUARD_INGEST_KEY","demo-ingest-key");
    if (!requiredKey.equals(apiKey)) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED,"Valid X-API-Key required");
  }
  @PatchMapping("/alerts/{id}") public SecurityEvent triage(@PathVariable Long id,@RequestBody TriageRequest request){
    SecurityEvent event=events.findById(id).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!List.of("OPEN","TRIAGED","RESOLVED").contains(request.status())) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid status");
    event.triage(request.status()); return events.save(event);
  }
  public record IngestRequest(@NotBlank String sourceIp,@NotBlank String username,@NotBlank String eventType,@PositiveOrZero long bytesTransferred,String customerId,String assetName){}
  public record BatchIngestRequest(@NotEmpty @Size(max=100) List<@Valid IngestRequest> events){}
  public record BatchIngestResponse(int accepted,List<SecurityEvent> events){}
  public record ScenarioResponse(String scenario,int accepted,List<SecurityEvent> events){}
  public record TriageRequest(@NotBlank String status){}
  public record WebsiteCheckRequest(@NotBlank String url){}
}
