<#
.SYNOPSIS
    Refresh backend/vendor-repo - the checked-in Maven repository holding the
    shared engine artifacts this module builds against.

.DESCRIPTION
    Why this exists: the engine is published to GitHub Packages, which needs a
    token and a reachable registry. While publishing is blocked, or on any
    machine without a `read:packages` token, `mvnw package` would fail with a 401.
    vendor-repo removes that dependency - same approach as
    frontend/vendor/*.tgz for the npm engine.

    Copies the versions backend/pom.xml declares out of the local ~/.m2 into a
    proper Maven repository layout, with sha1 sidecars.

    IT ALSO VENDORS THE TRANSITIVE ENGINE-SPRING. csnx-engine-ai pins its own
    csnx-engine-spring version, and Maven must read that pom to build the
    dependency graph even when the direct dependency wins mediation. Miss it and
    the build fails with a 401 on a version nobody appears to be asking for -
    which only shows up on a machine that has no token, i.e. someone else's.

    Prerequisite: the versions must already be in ~/.m2. Build them from the
    platform checkout first if they are not:

        cd <platform>/backend
        .\mvnw.cmd -f ..\engine-spring\pom.xml -DskipTests install
        .\mvnw.cmd -f ..\engine-ai\pom.xml     -DskipTests install

    ONCE THE ENGINE IS ON A REACHABLE REGISTRY, delete backend/vendor-repo and
    the `vendored-engine` <repository> entry in backend/pom.xml. Nothing else
    changes; this is deliberately easy to undo.

.PARAMETER Prune
    Delete vendored versions the pom no longer references. Off by default so an
    accidental run cannot strip something still in use.

.EXAMPLE
    .\scripts\vendor-engine.ps1

.EXAMPLE
    .\scripts\vendor-engine.ps1 -Prune
#>
[CmdletBinding()]
param([switch] $Prune)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Step($m) { Write-Host "==> $m" -ForegroundColor Cyan }
function Ok($m)   { Write-Host "  + $m" -ForegroundColor Green }
function Warn($m) { Write-Host "  ! $m" -ForegroundColor Yellow }
function Fail($m) { Write-Host "  x $m" -ForegroundColor Red; throw $m }

$RepoRoot   = Split-Path $PSScriptRoot -Parent
$BackendPom = Join-Path $RepoRoot 'backend\pom.xml'
$VendorRepo = Join-Path $RepoRoot 'backend\vendor-repo'
$M2         = Join-Path $env:USERPROFILE '.m2\repository'

if (-not (Test-Path $BackendPom)) { Fail "no $BackendPom" }

Step 'Versions declared in backend/pom.xml'
$xml = [xml](Get-Content $BackendPom)
$wanted = [ordered]@{}
foreach ($id in @('csnx-engine-spring', 'csnx-engine-ai')) {
    $dep = $xml.project.dependencies.dependency | Where-Object { $_.artifactId -eq $id }
    if ($dep) { $wanted[$id] = [string]$dep.version; Ok "$id $($dep.version)" }
}
if ($wanted.Count -eq 0) { Fail 'no engine dependencies found in backend/pom.xml' }

# The transitive pin. engine-ai carries its own engine-spring version and Maven
# reads that pom regardless of which version wins.
$extraSpring = $null
if ($wanted['csnx-engine-ai']) {
    $aiPom = Join-Path $M2 "za\co\csnx\csnx-engine-ai\$($wanted['csnx-engine-ai'])\csnx-engine-ai-$($wanted['csnx-engine-ai']).pom"
    if (Test-Path $aiPom) {
        $ai = [xml](Get-Content $aiPom)
        $d = $ai.project.dependencies.dependency | Where-Object { $_.artifactId -eq 'csnx-engine-spring' }
        if ($d -and [string]$d.version -ne $wanted['csnx-engine-spring']) {
            $extraSpring = [string]$d.version
            Warn "csnx-engine-ai pins csnx-engine-spring $extraSpring - vendoring it too (Maven reads its pom)"
        }
    } else {
        Warn "csnx-engine-ai pom not in ~/.m2 yet; cannot check its transitive engine-spring pin"
    }
}

$targets = @()
foreach ($k in $wanted.Keys) { $targets += [pscustomobject]@{ Artifact = $k; Version = $wanted[$k] } }
if ($extraSpring) { $targets += [pscustomobject]@{ Artifact = 'csnx-engine-spring'; Version = $extraSpring } }

Step "Copying into $VendorRepo"
foreach ($t in $targets) {
    $src = Join-Path $M2 "za\co\csnx\$($t.Artifact)\$($t.Version)"
    if (-not (Test-Path $src)) {
        Fail "$($t.Artifact) $($t.Version) is not in ~/.m2. Install it from the platform checkout first - see the header of this script."
    }
    $dst = Join-Path $VendorRepo "za\co\csnx\$($t.Artifact)\$($t.Version)"
    New-Item -ItemType Directory -Force -Path $dst | Out-Null

    # jar + pom ONLY. _remote.repositories and *.lastUpdated are resolver
    # internals; copying them makes Maven distrust the vendored copy.
    $copied = 0
    foreach ($f in Get-ChildItem $src -File | Where-Object { $_.Extension -in '.jar', '.pom' }) {
        Copy-Item $f.FullName (Join-Path $dst $f.Name) -Force
        $h = (Get-FileHash -Algorithm SHA1 -Path (Join-Path $dst $f.Name)).Hash.ToLower()
        [System.IO.File]::WriteAllText((Join-Path $dst "$($f.Name).sha1"), $h,
            (New-Object System.Text.UTF8Encoding($false)))
        $copied++
    }
    if ($copied -eq 0) { Fail "no jar/pom found in $src" }
    Ok "$($t.Artifact) $($t.Version)  ($copied file(s))"
}

if ($Prune) {
    Step 'Pruning versions the pom no longer references'
    $keep = $targets | ForEach-Object { "$($_.Artifact)/$($_.Version)" }
    foreach ($d in Get-ChildItem (Join-Path $VendorRepo 'za\co\csnx') -Directory -ErrorAction SilentlyContinue) {
        foreach ($v in Get-ChildItem $d.FullName -Directory) {
            if ("$($d.Name)/$($v.Name)" -notin $keep) {
                Remove-Item $v.FullName -Recurse -Force
                Warn "pruned $($d.Name) $($v.Name)"
            }
        }
    }
}

$size = (Get-ChildItem $VendorRepo -Recurse -File | Measure-Object -Sum Length).Sum
Step ("vendor-repo is now {0:N0} KB" -f ($size / 1KB))

Write-Host @"

Verify it works the way it needs to - with no token and nothing cached:

  # temporarily move the engine out of ~/.m2, then:
  cd backend
  .\mvnw.cmd -s <a settings.xml with no github-csnx server> -DskipTests compile

That must succeed. If it 401s on a version you did not expect, it is the
transitive engine-spring pin inside csnx-engine-ai - re-run this script.
"@
