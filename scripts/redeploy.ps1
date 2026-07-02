<#
.SYNOPSIS
    Rebuild this module from the latest main and (re)start its updated containers.

.DESCRIPTION
    The full local redeploy for the repo this script lives in (resolved from its
    own path - identical across platform / pom / demo):

      1. (unless -NoPull) fast-forward the local checkout to origin/main.
      2. (unless -NoJar)  build the backend fat-jar on the host with JDK 21
         (`backend/mvnw package`). REQUIRED: the backend Dockerfile COPYs a
         pre-built target/*-backend-*.jar - without this step the container
         ships a stale jar.
      3. `docker compose up -d --build` so the backend image picks up the new
         jar and the frontend image re-runs `npm ci` against the vendored
         engine + builds a fresh Vite bundle (the running frontend serves the
         bundle baked at image-build time, so a rebuild is the only way the UI
         changes land).
      4. show the resulting `docker compose ps`.

    Shared-engine changes are NOT built here - run scripts/build-shared.ps1
    first (or redeploy-all.ps1 -Shared) so the new artifacts exist before the
    images rebuild against them.

.PARAMETER NoPull   Skip the git fast-forward; rebuild the current checkout.
.PARAMETER NoJar    Skip the host `mvnw package` (e.g. frontend-only change).
.PARAMETER Service  Limit the image rebuild to one service (backend | frontend).
.PARAMETER SkipTests  Passed through to maven (default: tests are skipped for speed).

.EXAMPLE
    ./scripts/redeploy.ps1                       # pull main, rebuild all, restart
.EXAMPLE
    ./scripts/redeploy.ps1 -NoPull -Service frontend   # frontend-only rebuild
#>
[CmdletBinding()]
param(
    [switch] $NoPull,
    [switch] $NoJar,
    [ValidateSet('backend', 'frontend')] [string] $Service,
    [bool] $SkipTests = $true
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path $PSScriptRoot -Parent
$Name = Split-Path $RepoRoot -Leaf

function Step($m) { Write-Host "==> [$Name] $m" -ForegroundColor Cyan }
function Ok($m)   { Write-Host "  + $m" -ForegroundColor Green }
function Warn($m) { Write-Host "  ! $m" -ForegroundColor Yellow }

function Resolve-Jdk21 {
    # Filesystem detection first (no process spawn) - the default JAVA_HOME on
    # this machine is JDK 8, and probing it via `java -version` would write to
    # stderr (a terminating error under ErrorActionPreference=Stop).
    $c = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -like 'jdk-21*' } | Sort-Object Name -Descending | Select-Object -First 1
    if ($c) { return $c.FullName }
    # Fall back to JAVA_HOME only if it already points at a 21 JDK.
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
        $old = $ErrorActionPreference; $ErrorActionPreference = 'SilentlyContinue'
        $v = & "$env:JAVA_HOME\bin\java.exe" -version 2>&1 | Out-String
        $ErrorActionPreference = $old
        if ($v -match 'version "21') { return $env:JAVA_HOME }
    }
    throw "JDK 21 not found. Set JAVA_HOME to a Temurin 21 JDK (see scripts/README.md)."
}

Push-Location $RepoRoot
try {
    # 1. Latest main ---------------------------------------------------------
    if (-not $NoPull) {
        Step "fast-forwarding to origin/main"
        $branch = (& git rev-parse --abbrev-ref HEAD).Trim()
        if ($branch -ne 'main') { Warn "on '$branch', not 'main' - pulling main into it may fail; use -NoPull to skip" }
        & git pull --ff-only origin main
        if ($LASTEXITCODE -ne 0) {
            Warn "git pull --ff-only failed (uncommitted changes or diverged) - continuing with current checkout"
        } else { Ok "checkout up to date" }
    } else { Warn "skipping git pull (-NoPull)" }

    # 2. Backend jar on the host (JDK 21) ------------------------------------
    if (-not $NoJar -and $Service -ne 'frontend') {
        Step "building backend jar (mvnw package)"
        $jdk = Resolve-Jdk21
        $prev = $env:JAVA_HOME; $env:JAVA_HOME = $jdk
        Write-Host "  JAVA_HOME=$jdk" -ForegroundColor Gray
        $mvnArgs = @('-q', 'package'); if ($SkipTests) { $mvnArgs += '-DskipTests' }
        Push-Location (Join-Path $RepoRoot 'backend')
        try {
            & .\mvnw.cmd @mvnArgs
            if ($LASTEXITCODE -ne 0) { throw "backend build failed ($LASTEXITCODE)" }
        } finally { Pop-Location; $env:JAVA_HOME = $prev }
        Ok "backend jar built"
    } else { Warn "skipping backend jar build" }

    # 2b. GitHub Packages token file for the frontend image build ------------
    # docker-compose.yml feeds `npm ci` the file-based secret .secrets/gh_token
    # (file-based because env-sourced build secrets need compose >= 2.23).
    # Materialise it from GH_PACKAGES_TOKEN - process env first, then the
    # User-scope variable (set on this machine but not inherited by
    # non-login shells).
    $ghToken = $env:GH_PACKAGES_TOKEN
    if (-not $ghToken) { $ghToken = [Environment]::GetEnvironmentVariable('GH_PACKAGES_TOKEN', 'User') }
    if ($ghToken) {
        New-Item -ItemType Directory -Force -Path (Join-Path $RepoRoot '.secrets') | Out-Null
        [System.IO.File]::WriteAllText((Join-Path $RepoRoot '.secrets\gh_token'), $ghToken)
        Ok "wrote .secrets/gh_token for the image build"
    } elseif (-not (Test-Path (Join-Path $RepoRoot '.secrets\gh_token'))) {
        throw "GH_PACKAGES_TOKEN not set and .secrets/gh_token missing - the frontend image build needs a GitHub Packages read token"
    }

    # 3. Rebuild + restart containers ----------------------------------------
    Step "docker compose up -d --build"
    # Build a single args array and splat it. (A 1-element @($Service) gets
    # unwrapped to a scalar string on assignment, and splatting a string
    # enumerates its characters — so append onto a multi-element array instead.)
    $composeArgs = @('up', '-d', '--build')
    if ($Service) { $composeArgs += $Service }
    & docker compose @composeArgs
    if ($LASTEXITCODE -ne 0) { throw "docker compose up failed ($LASTEXITCODE)" }

    # 4. Status --------------------------------------------------------------
    Step "stack status"
    & docker compose ps
    Ok "redeploy complete"
}
finally { Pop-Location }
