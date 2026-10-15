package com.threatguard.api;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class WebsiteHealthService {
  private final HttpClient client = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .followRedirects(HttpClient.Redirect.NORMAL)
      .build();

  public WebsiteHealth check(String rawUrl) {
    URI uri;
    try {
      uri = URI.create(rawUrl.trim());
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("Enter a valid website URL.");
    }

    if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
      throw new IllegalArgumentException("Only HTTP and HTTPS website URLs are supported.");
    }

    long started = System.nanoTime();
    try {
      HttpRequest request = HttpRequest.newBuilder(uri)
          .timeout(Duration.ofSeconds(10))
          .header("User-Agent", "ThreatGuard-Student-Monitor/1.0")
          .GET()
          .build();
      HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
      long responseTime = Duration.ofNanos(System.nanoTime() - started).toMillis();
      int status = response.statusCode();
      String health = status >= 200 && status < 400 ? "HEALTHY" : "ERROR";
      return new WebsiteHealth(uri.toString(), health, status, responseTime, "HTTP " + status);
    } catch (Exception exception) {
      long responseTime = Duration.ofNanos(System.nanoTime() - started).toMillis();
      return new WebsiteHealth(uri.toString(), "UNREACHABLE", 0, responseTime, exception.getClass().getSimpleName());
    }
  }

  public record WebsiteHealth(String url, String status, int httpStatus, long responseTimeMs, String message) {}
}
