<#
.SYNOPSIS
  Open a local JDBC port (127.0.0.1:5432) to the demo Cloud SQL instance via
  Cloud SQL Auth Proxy.

.DESCRIPTION
  Cloud SQL has no public IP, so connecting from a laptop requires the Cloud
  SQL Auth Proxy (which tunnels over IAM-authenticated HTTPS). On first run
  this script downloads the proxy binary into ./tools/. On every run it:

    1. Prints the demo_app password from Secret Manager (one-shot, no copy).
    2. Starts the proxy in the foreground listening on 127.0.0.1:5432.

  Connect with any Postgres client:
    Host:     127.0.0.1
    Port:     5432
    DB:       demo
    User:     demo_app
    Password: (printed above)
    JDBC URL: jdbc:postgresql://127.0.0.1:5432/demo?sslmode=disable

  Ctrl+C closes the tunnel.

.PARAMETER LocalPort
  Local port to bind. Default 5432. Use another port if you also run a local
  Postgres (e.g. via docker-compose) on 5432.
#>
[CmdletBinding()]
param(
    [int]$LocalPort = 5432
)

$ErrorActionPreference = 'Stop'

$Project  = 'ai-development-459111'
$Region   = 'africa-south1'
$Instance = "${Project}:${Region}:demo-pg"

$ToolsDir  = Join-Path $PSScriptRoot 'tools'
$ProxyExe  = Join-Path $ToolsDir 'cloud-sql-proxy.exe'
$ProxyUrl  = 'https://storage.googleapis.com/cloud-sql-connectors/cloud-sql-proxy/v2.13.0/cloud-sql-proxy.x64.exe'

if (-not (Test-Path $ProxyExe)) {
    New-Item -ItemType Directory -Path $ToolsDir -Force | Out-Null
    Write-Host "Downloading Cloud SQL Auth Proxy v2.13.0..." -ForegroundColor Cyan
    Invoke-WebRequest -Uri $ProxyUrl -OutFile $ProxyExe -UseBasicParsing
    Write-Host "Saved to $ProxyExe" -ForegroundColor Green
}

Write-Host ''
Write-Host 'demo_app password (from Secret Manager):' -ForegroundColor Yellow
gcloud secrets versions access latest --secret=demo-db-password
Write-Host ''
Write-Host "Connect to: jdbc:postgresql://127.0.0.1:$LocalPort/demo?sslmode=disable" -ForegroundColor Yellow
Write-Host "User: demo_app" -ForegroundColor Yellow
Write-Host ''
Write-Host "Starting proxy on 127.0.0.1:$LocalPort -> $Instance ..." -ForegroundColor Cyan
Write-Host '(Ctrl+C to stop)'
Write-Host ''

& $ProxyExe --address 127.0.0.1 --port $LocalPort $Instance
