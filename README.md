# ThreatGuard Real-Time Threat and Anomaly Detection

Java Spring Boot + Weka + relational telemetry + SOC dashboard.

## Run locally

```powershell
cd backend
.\mvnw.cmd spring-boot:run

cd frontend
python -m http.server 8000
```

Open http://localhost:8000.

The complete source, four-member organization, report DOCX/PDF, and testing materials are included in ThreatGuard_Project_Source.zip.

## Team

- Mayank Singh - team leadership and backend integration
- Bhashkar Pathak - frontend SOC dashboard
- Raj Farswan - Weka machine learning
- Pawan Panday - database, security, and testing

This repository is a safe academic prototype. Demo attacks generate synthetic local telemetry only.
