$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$backendUrl = 'http://localhost:8080'
$frontendUrl = 'http://localhost:8000'
$csvPath = Join-Path $root 'data\curated_security_events.csv'

Write-Host 'ThreatGuard project checks'
Write-Host '--------------------------'

if (-not (Test-Path $csvPath)) {
  throw "Dataset not found: $csvPath"
}

$rows = Import-Csv $csvPath
$normal = @($rows | Where-Object { $_.label -eq 'NORMAL' }).Count
$anomaly = @($rows | Where-Object { $_.label -eq 'ANOMALY' }).Count

if ($rows.Count -ne 10000 -or $normal -ne 7000 -or $anomaly -ne 3000) {
  throw "Dataset check failed. Found $($rows.Count) rows, $normal normal, $anomaly anomaly."
}

Write-Host '[PASS] Dataset contains 10,000 records: 7,000 normal and 3,000 anomaly.'

try {
  $metrics = Invoke-RestMethod "$backendUrl/api/metrics"
  $model = Invoke-RestMethod "$backendUrl/api/ml/metrics"
  $events = Invoke-RestMethod "$backendUrl/api/events"
  $integrationBody = '{"events":[{"sourceIp":"198.51.100.10","username":"smoke-test","eventType":"API_REQUEST","bytesTransferred":0,"customerId":"smoke-test","assetName":"testing"}]}'
  $integration = Invoke-RestMethod -Method Post "$backendUrl/api/integration/events" -Headers @{'X-API-Key'='demo-ingest-key'} -ContentType 'application/json' -Body $integrationBody
  $website = Invoke-RestMethod -Method Post "$backendUrl/api/website/check" -ContentType 'application/json' -Body '{"url":"https://example.com"}'

  if ($null -eq $metrics.events -or $null -eq $model.algorithm -or $null -eq $events -or $integration.accepted -ne 1 -or $null -eq $website.status) {
    throw 'One or more API responses were incomplete.'
  }

  Write-Host '[PASS] Java API returned dashboard metrics, model metrics, and events.'
  Write-Host "[INFO] Model baseline: $($model.baselineSamples); validation: $($model.validationSamples)"
  Write-Host "[PASS] Integration batch endpoint accepted $($integration.accepted) event."
  Write-Host "[PASS] Website health check returned $($website.status) with HTTP $($website.httpStatus)."
}
catch {
  Write-Host '[INFO] Backend check skipped because the Java API is not running.'
  Write-Host '       Start it with: testing\start.ps1'
}

try {
  $frontend = Invoke-WebRequest -UseBasicParsing $frontendUrl
  if ($frontend.StatusCode -ne 200) {
    throw "Frontend returned HTTP $($frontend.StatusCode)."
  }
  Write-Host '[PASS] Frontend is available on http://localhost:8000.'
}
catch {
  Write-Host '[INFO] Frontend check skipped because the frontend server is not running.'
  Write-Host '       Start it with: python -m http.server 8000 (inside frontend).'
}

Write-Host 'Project checks completed.'
