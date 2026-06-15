<#
.SYNOPSIS
    Start / stop / inspect this module's container stack (postgres + backend + frontend).

.DESCRIPTION
    Thin wrapper around `docker compose` for the repo this script lives in
    (resolved from its own location, so it works from any working directory).
    No rebuild - use redeploy.ps1 to rebuild images. Identical across the
    platform / pom / demo repos.

.PARAMETER Action
    up       start containers (create if missing), detached
    down     stop AND remove containers + network (keeps the named DB volume)
    stop     stop containers, leave them defined
    start    start previously-stopped containers
    restart  restart running containers
    status   show `docker compose ps`
    logs     tail logs (use -Follow to stream, -Service to filter)

.PARAMETER Service
    Limit the action to a single service: postgres | backend | frontend.

.PARAMETER Follow
    For `logs`: stream (-f) instead of a one-shot tail.

.EXAMPLE
    ./scripts/stack.ps1 -Action up
.EXAMPLE
    ./scripts/stack.ps1 -Action logs -Service backend -Follow
.EXAMPLE
    ./scripts/stack.ps1 -Action down        # tear down but keep the DB volume
#>
[CmdletBinding()]
param(
    [ValidateSet('up', 'down', 'stop', 'start', 'restart', 'status', 'logs')]
    [string] $Action = 'status',
    [ValidateSet('postgres', 'backend', 'frontend')]
    [string] $Service,
    [switch] $Follow
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path $PSScriptRoot -Parent
$svc = if ($Service) { @($Service) } else { @() }

function Exec($argList) {
    Write-Host "docker compose $($argList -join ' ')" -ForegroundColor Cyan
    & docker compose @argList
    if ($LASTEXITCODE -ne 0) { throw "docker compose failed ($LASTEXITCODE)" }
}

Push-Location $RepoRoot
try {
    switch ($Action) {
        'up'      { Exec (@('up', '-d') + $svc) }
        'down'    { Exec @('down') }                       # volume is named -> survives
        'stop'    { Exec (@('stop') + $svc) }
        'start'   { Exec (@('start') + $svc) }
        'restart' { Exec (@('restart') + $svc) }
        'status'  { Exec @('ps') }
        'logs'    {
            $a = @('logs', '--tail', '200')
            if ($Follow) { $a += '-f' }
            Exec ($a + $svc)
        }
    }
}
finally { Pop-Location }
