# Connecting a real application

ThreatGuard can receive security events from an application, website, or server.
The sending application must include the same API key configured in the
`THREATGUARD_INGEST_KEY` environment variable. If no variable is configured,
the local development key is `demo-ingest-key`.

## Send one event

```powershell
$body = @{
  sourceIp = '203.0.113.10'
  username = 'customer-42'
  eventType = 'FAILED_LOGIN'
  bytesTransferred = 0
  customerId = 'customer-portal'
  assetName = 'login-api'
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri 'http://localhost:8080/api/events' `
  -Headers @{ 'X-API-Key' = 'demo-ingest-key' } `
  -ContentType 'application/json' `
  -Body $body
```

The event is scored, stored, and immediately visible in the dashboard's Demo
Data view.

## Send a batch

Use the integration endpoint when an application has collected several events.
One request can contain up to 100 events.

```json
{
  "events": [
    {
      "sourceIp": "203.0.113.10",
      "username": "customer-42",
      "eventType": "FAILED_LOGIN",
      "bytesTransferred": 0,
      "customerId": "customer-portal",
      "assetName": "login-api"
    }
  ]
}
```

Send that JSON to `POST /api/integration/events` with the `X-API-Key` header.
The response contains the accepted count and the scored event records.

## Production checklist

- Set a long random `THREATGUARD_INGEST_KEY`; never use the demo key.
- Use HTTPS and keep the API private or behind an authenticated gateway.
- Send only systems and users that have permission to report telemetry.
- Replace the default in-memory H2 database with PostgreSQL for persistent data.
