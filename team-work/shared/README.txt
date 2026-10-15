ThreatGuard testing
===================

Project features
----------------

- Live dashboard with event search and severity filtering.
- Explainable scoring using failed logins, privileged accounts, suspicious IP
  ranges, off-hours activity, and unusual data volume.
- Weka K-Means anomaly detection trained from the included CSV dataset.
- Website availability and response-time health checks.
- Threat Lab buttons for brute-force, data-exfiltration, privilege-abuse, and
  normal-activity demonstrations.
- Alert triage: OPEN, TRIAGED, and RESOLVED.

Four-member work split
----------------------

Member 1 - Frontend: frontend/index.html, frontend/styles.css,
frontend/app.js, dashboard usability, filters, and alert views.

Member 2 - Backend API: backend/src/main/java/api/SecurityEvent.java,
EventRepository.java, EventService.java, and ThreatController.java.

Member 3 - Machine learning: WekaAnomalyService.java, ModelMetrics.java,
data/curated_security_events.csv, and model evaluation.

Member 4 - Integration and testing: AnomalyScorer.java, TelemetrySimulator.java,
WebsiteHealthService.java, application.yml, startup scripts, and smoke tests.

Run all simple project checks from the project root:

    powershell -ExecutionPolicy Bypass -File testing\check-project.ps1

The script checks:

- The dataset exists and contains 10,000 records.
- The dataset contains 7,000 NORMAL and 3,000 ANOMALY records.
- The Java API returns events and model metrics when the backend is running.
- The frontend is reachable when the frontend server is running.

Threat Lab API examples
-----------------------

After starting the backend, run:

    curl -X POST http://localhost:8080/api/demo/scenarios/brute-force

Valid scenarios are brute-force, data-exfiltration, privilege-abuse, and
normal-activity. The endpoint creates synthetic records only; it does not
connect to or attack any external system.

Start the complete project first with:

    powershell -ExecutionPolicy Bypass -File testing\start.ps1
