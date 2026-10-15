$projectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$backendDir = Join-Path $projectRoot 'backend'
$frontendDir = Join-Path $projectRoot 'frontend'

Write-Host 'Starting ThreatGuard backend...'
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$backendDir'; .\mvnw.cmd spring-boot:run" | Out-Null

Write-Host 'Starting ThreatGuard frontend...'
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$frontendDir'; python -m http.server 8000" | Out-Null

Write-Host 'Open http://localhost:8000 in your browser'
Write-Host 'Backend is running at http://localhost:8080'
