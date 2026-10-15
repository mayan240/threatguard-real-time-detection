package com.threatguard.api;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
@Component
public class TelemetrySimulator {
  private final EventService eventService;
  TelemetrySimulator(EventService eventService){this.eventService=eventService;}
  @Scheduled(fixedDelay = 3500, initialDelay = 1000) public void emit(){
    var r=ThreadLocalRandom.current(); String ip=List.of("10.0.0."+r.nextInt(2,255),"172.16.4."+r.nextInt(2,255),"185.220.101."+r.nextInt(2,255),"45.142.214."+r.nextInt(2,255)).get(r.nextInt(4));
    String type=List.of("LOGIN_SUCCESS","FILE_ACCESS","API_REQUEST","FAILED_LOGIN").get(r.nextInt(4)); String user=List.of("mayank","analyst","admin","root").get(r.nextInt(4)); long bytes=r.nextInt(500, 9_000_000);
    eventService.ingest(ip,user,type,bytes,"demo-workspace",List.of("auth-gateway","payments-api","customer-db").get(r.nextInt(3)));
  }
}
