param(
  [int]$BackendPort = 18080,
  [int]$FrontendPort = 15173
)
$ErrorActionPreference = 'Stop'
function Assert-PortFree([int]$Port) {
  $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $Port)
  try { $listener.Start() } catch { throw "Port $Port is already in use; no process was stopped." } finally { try { $listener.Stop() } catch {} }
}
Assert-PortFree $BackendPort
$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root 'backend'
$frontend = Join-Path $root 'frontend'
$run = Join-Path $root '.acceptance-run'
New-Item -ItemType Directory -Force $run | Out-Null
$imageDir = Join-Path $run 'images'
New-Item -ItemType Directory -Force $imageDir | Out-Null
$dbUrl = "jdbc:h2:mem:image_acceptance;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1"
$env:SPRING_PROFILES_ACTIVE = 'acceptance'
$env:SPRING_DATASOURCE_URL = $dbUrl
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME = 'org.h2.Driver'
$env:SPRING_DATASOURCE_USERNAME = 'sa'
$env:SPRING_DATASOURCE_PASSWORD = ''
$env:SPRING_FLYWAY_ENABLED = 'false'
$env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'none'
$env:IMAGE_STORAGE_DIRECTORY = $imageDir
$env:SERVER_PORT = "$BackendPort"
$env:JWT_SECRET = 'acceptance-only-secret-change-me-32-chars'
$env:AI_PROVIDER = 'mock'
$env:AI_MOCK_ENABLED = 'true'
$env:ZHIPU_IMAGE_API_KEY = ''
Write-Host "Acceptance backend: http://localhost:$BackendPort"
Write-Host "Acceptance frontend: http://localhost:$FrontendPort (run Vite with --mode acceptance)"
Write-Host "Register once through the UI/API: acceptance@example.test / Accept123!"
Write-Host "Run from another terminal: powershell -ExecutionPolicy Bypass -File .\scripts\start-image-acceptance-frontend.ps1"
Push-Location $backend
try { mvn "-Dmaven.repo.local=$root\..\repo-cache" "-DforkCount=0" "-Dtest=AcceptanceServerLauncher" test } finally { Pop-Location }
