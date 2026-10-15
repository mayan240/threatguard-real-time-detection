# Four-member project file organization

The runnable project remains in the original `backend`, `frontend`, and
`testing` folders. The folders below are an ownership view of the same work;
they contain copies of the relevant files so each member can review or present
their area without breaking the Maven project layout.

## Member 1 - Mayank Singh, Team Leader

Folder: `01-Mayank-Singh-Team-Leader/backend-integration`

Owns Spring Boot startup, existing event APIs, controller integration, and
final end-to-end integration.

Key files:

- `ThreatGuardApplication.java`
- `ThreatController.java`
- `EventService.java`
- `EventRepository.java`
- `SecurityEvent.java`

## Member 2 - Bhashkar Pathak, Frontend and SOC Dashboard

Folder: `02-Bhashkar-Pathak-Frontend`

Owns dashboard layout, API calls, event views, filters, alert display, and the
Threat Lab simulator controls.

Key files:

- `index.html`
- `app.js`
- `styles.css`
- `training-data.html`
- `training-security-events.csv`

## Member 3 - Raj Farswan, Machine Learning and Weka

Folder: `03-Raj-Farswan-Machine-Learning`

Owns the ARFF feature schema, Weka training and inference, model metrics,
explainability, and ML training data.

Key files:

- `WekaAnomalyService.java`
- `AnomalyScorer.java`
- `ModelMetrics.java`
- `cyber_traffic.arff`
- `training-security-events.csv`

## Member 4 - Pawan Panday, Database, Security, and Testing

Folder: `04-Pawan-Panday-Database-Security-Testing`

Owns telemetry persistence, blocklist enforcement, WAF filtering, SOC
operations, audit logging, schema configuration, and security testing.

Key files:

- `RequestLog.java` and `RequestLogRepository.java`
- `BlockedIp.java` and `BlockedIpRepository.java`
- `SecurityAlert.java` and `SecurityAlertRepository.java`
- `AdminAuditLog.java` and `AdminAuditLogRepository.java`
- `SecurityTelemetryFilter.java`
- `TelemetryFeatureAggregator.java`
- `SocOperationsController.java`
- `DemoAttackController.java`
- `schema.sql`
- `application.yml`

## Shared integration files

Folder: `shared`

Contains the Maven build files, common testing scripts, health-check service,
and project run instructions.

## Important workflow rule

Make code changes in the canonical `backend` and `frontend` folders first.
The `team-work` folders are organized ownership copies for review, submission,
and presentation. After a change is accepted, refresh the relevant ownership
copy so the two views remain consistent.
