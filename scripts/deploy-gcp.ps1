<#
.SYNOPSIS
  Build, push, and roll a Cloud Run revision for the demo backend, frontend, or both.

.DESCRIPTION
  Reads project/region/repo from constants below (matching .gcp/deployment-plan.md
  and infra/gcp/terraform.tfvars). For each requested service, runs:
    docker build  -> Artifact Registry image tag
    docker push   -> uploads to Artifact Registry
    gcloud run services update --image  -> rolls a new Cloud Run revision

  Infra changes are NOT touched by this script — those go through
  `terraform apply` in infra/gcp.

.PARAMETER Service
  Which service to redeploy: backend, frontend, or both (default).

.EXAMPLE
  .\scripts\deploy-gcp.ps1
  Redeploys both services.

.EXAMPLE
  .\scripts\deploy-gcp.ps1 backend
  Redeploys only the backend (faster after a Java change).
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [ValidateSet('backend', 'frontend', 'both')]
    [string]$Service = 'both'
)

$ErrorActionPreference = 'Stop'

# The frontend image uses BuildKit features (--mount=type=secret for the GitHub
# Packages token, # syntax= directive). BuildKit isn't the default on Docker
# < 23, so force it on.
$env:DOCKER_BUILDKIT = '1'

$Project = 'ai-development-459111'
$Region  = 'africa-south1'
$Repo    = "$Region-docker.pkg.dev/$Project/demo-docker"

# Central platform IdP public URL — the frontend reads it at runtime
# (PLATFORM_ISSUER env -> config.js, rendered by the container entrypoint).
# Deterministic project-number Cloud Run URL (project 236510297424). Must match
# the PLATFORM_ISSUER env set on the demo-backend Cloud Run service in infra/gcp.
$IssuerUrl = 'https://platform-backend-236510297424.africa-south1.run.app'

# Central portal URL — runtime env too (PORTAL_URL); the OIDC RP-initiated
# logout redirects here (must be a registered post-logout URI).
$PortalUrl = 'https://platform-frontend-236510297424.africa-south1.run.app'

# Resolve repo root from this script's location so it works from anywhere.
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

function Invoke-DeployService {
    param(
        [Parameter(Mandatory)] [string]$Name,
        [Parameter(Mandatory)] [string]$ContextDir
    )

    $image = "$Repo/demo-$Name`:latest"
    $context = Join-Path $RepoRoot $ContextDir
    $svc = "demo-$Name"

    Write-Host ''
    Write-Host "==> [$Name] build  $image" -ForegroundColor Cyan
    if ($Name -eq 'frontend') {
        # Frontend needs the GitHub Packages token (engine pulled at npm ci).
        # Nothing environment-specific is baked in — the issuer + portal URL
        # are runtime env vars.
        docker build --secret "id=gh_token,env=GH_PACKAGES_TOKEN" -t $image $context
    }
    else {
        docker build -t $image $context
    }
    if ($LASTEXITCODE -ne 0) { throw "docker build failed for $Name" }

    Write-Host "==> [$Name] push   $image" -ForegroundColor Cyan
    docker push $image
    if ($LASTEXITCODE -ne 0) { throw "docker push failed for $Name" }

    Write-Host "==> [$Name] roll   Cloud Run revision" -ForegroundColor Cyan
    if ($Name -eq 'frontend') {
        # Terraform owns these env vars too (same values); setting them on the
        # roll keeps the frontend working even if this image lands before the
        # next `terraform apply` stamps them.
        gcloud run services update $svc --region $Region --image $image --update-env-vars "PLATFORM_ISSUER=$IssuerUrl,PORTAL_URL=$PortalUrl"
    }
    else {
        gcloud run services update $svc --region $Region --image $image
    }
    if ($LASTEXITCODE -ne 0) { throw "gcloud run update failed for $Name" }

    Write-Host "==> [$Name] done" -ForegroundColor Green
}

if ($Service -in 'backend', 'both') {
    Invoke-DeployService -Name 'backend' -ContextDir 'backend'
}
if ($Service -in 'frontend', 'both') {
    Invoke-DeployService -Name 'frontend' -ContextDir 'frontend'
}

Write-Host ''
Write-Host 'All requested services redeployed.' -ForegroundColor Green
Write-Host 'Frontend: https://demo-frontend-tonnt75awq-bq.a.run.app'
