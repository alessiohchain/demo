<#
.SYNOPSIS
  Build, push, and roll a Container Apps revision for the demo backend,
  frontend, or both — the Azure sibling of deploy-gcp.ps1.

.DESCRIPTION
  Discovers the shared fleet resources (ACR, Container Apps environment) from
  the resource group at runtime — their names carry a random suffix, so
  nothing is hardcoded. For each requested service:
    backend  : ./mvnw package -> docker build -> docker push -> az containerapp update
    frontend : docker build (engine pulled from GitHub Packages via BuildKit
               secret; issuer + portal URL are the runtime PLATFORM_ISSUER /
               PORTAL_URL env vars — nothing is baked) -> push -> update

  Images are tagged with the git short SHA (plus :latest) and revisions are
  rolled to the SHA tag — pushing the same :latest would not create a new
  revision on ACA. Terraform ignores image drift (lifecycle ignore_changes).

  Infra (the apps, identities, role assignments) is NOT touched here — that
  goes through `terraform apply` in infra/azure. The shared fleet (ACR,
  environment, Postgres, Key Vault, the demo_app role + demo-db-password
  secret) is owned by the PLATFORM repo's infra/azure.

  First-time bootstrap (the shared fleet must already exist — platform's
  terraform + db-init have run):
    1. .\scripts\deploy-azure.ps1 -NoRoll        # build + push images
    2. terraform apply                            # infra/azure — creates the apps
  Later deploys: .\scripts\deploy-azure.ps1 [backend|frontend|both]

.PARAMETER Service
  Which service to (re)deploy: backend, frontend, or both (default).

.PARAMETER NoRoll
  Build + push only; skip the Container Apps revision roll.

.PARAMETER IssuerUrl
  Override the platform OIDC issuer set on the frontend app as the
  PLATFORM_ISSUER runtime env var (set this when a custom domain is bound to
  platform-backend). Default: the platform backend's FQDN derived from the
  Container Apps environment. Must match the PLATFORM_ISSUER env on the
  demo-backend Container App (infra/azure).

.PARAMETER PortalUrl
  Override the central portal URL set as the PORTAL_URL runtime env var (the
  OIDC RP-initiated logout redirects here; must be a registered post-logout
  URI). Default: the platform frontend's FQDN derived from the Container Apps
  environment.
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [ValidateSet('backend', 'frontend', 'both')]
    [string]$Service = 'both',

    [switch]$NoRoll,

    [string]$IssuerUrl = '',
    [string]$PortalUrl = '',

    # Fleet coordinates — override for shared-group deployments (e.g.
    # -ResourceGroup Playground). Discovery filters on FleetName so unrelated
    # resources in a shared group are never picked up.
    [string]$ResourceGroup = $(if ($env:CSNX_RESOURCE_GROUP) { $env:CSNX_RESOURCE_GROUP } else { 'csnx-rg' }),
    [string]$FleetName = $(if ($env:CSNX_FLEET_NAME) { $env:CSNX_FLEET_NAME } else { 'csnx' })
)

$ErrorActionPreference = 'Stop'

# The frontend image uses BuildKit features (--mount=type=secret for the GitHub
# Packages token, # syntax= directive). BuildKit isn't the default on Docker
# < 23, so force it on.
$env:DOCKER_BUILDKIT = '1'

# Resolve repo root from this script's location so it works from anywhere.
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

# --- Discover shared fleet resources (random-suffixed names) ---------------
# Filtered on the fleet prefix, NOT [0]: a shared resource group (e.g. a
# corporate "Playground") may hold unrelated registries or environments.

$AcrName = az acr list --resource-group $ResourceGroup --query "[?starts_with(name, '$FleetName')].name | [0]" -o tsv
if (-not $AcrName) { throw "No '$FleetName*' ACR found in $ResourceGroup — run the PLATFORM repo's scripts/deploy-infra-azure.ps1 first." }
$AcrServer = "$AcrName.azurecr.io"

$EnvDomain = az containerapp env list --resource-group $ResourceGroup --query "[?name=='$FleetName-aca-env'].properties.defaultDomain | [0]" -o tsv
if (-not $EnvDomain) { throw "No '$FleetName-aca-env' Container Apps environment found in $ResourceGroup — run the PLATFORM repo's scripts/deploy-infra-azure.ps1 first." }

if (-not $IssuerUrl) {
    # Must match the platform's default-FQDN issuer form. If a custom-domain
    # issuer is configured on the platform, pass -IssuerUrl.
    $IssuerUrl = "https://platform-backend.$EnvDomain"
}
if (-not $PortalUrl) {
    $PortalUrl = "https://platform-frontend.$EnvDomain"
}

# SHA tag so every roll is a distinct revision.
$Tag = (git -C $RepoRoot rev-parse --short HEAD 2>$null)
if (-not $Tag) { $Tag = Get-Date -Format 'yyyyMMddHHmmss' }

az acr login --name $AcrName | Out-Null
if ($LASTEXITCODE -ne 0) { throw "az acr login failed" }

function Test-AppExists {
    param([Parameter(Mandatory)] [string]$Name)
    az containerapp show --name $Name --resource-group $ResourceGroup --query name -o tsv 2>$null | Out-Null
    return ($LASTEXITCODE -eq 0)
}

function Invoke-DeployService {
    param(
        [Parameter(Mandatory)] [string]$Name,
        [Parameter(Mandatory)] [string]$ContextDir
    )

    $image   = "$AcrServer/demo-$Name`:$Tag"
    $latest  = "$AcrServer/demo-$Name`:latest"
    $context = Join-Path $RepoRoot $ContextDir
    $app     = "demo-$Name"

    if ($Name -eq 'backend') {
        # The backend Dockerfile copies a prebuilt target/demo-backend-*.jar,
        # so build the fat-jar on the host first (needs JDK 21; the shared
        # engine artifacts resolve from the host ~/.m2 or GitHub Packages).
        Write-Host "==> [backend] mvn package" -ForegroundColor Cyan
        Push-Location $context
        try {
            & (Join-Path $context 'mvnw.cmd') -q -ntp -DskipTests package
            if ($LASTEXITCODE -ne 0) { throw "mvn package failed" }
        }
        finally {
            Pop-Location
        }
    }

    Write-Host ''
    Write-Host "==> [$Name] build  $image" -ForegroundColor Cyan
    if ($Name -eq 'frontend') {
        # The engine npm package is currently VENDORED (package.json points at
        # file:vendor/*.tgz), so npm ci never contacts GitHub Packages. The
        # Dockerfile still mounts the gh_token BuildKit secret, so satisfy it
        # with a placeholder when no real token is set — restore a real
        # GH_PACKAGES_TOKEN when the registry dependency returns.
        if (-not $env:GH_PACKAGES_TOKEN) { $env:GH_PACKAGES_TOKEN = 'vendored-engine-no-token-needed' }
        docker build --secret "id=gh_token,env=GH_PACKAGES_TOKEN" -t $image -t $latest $context
    }
    else {
        docker build -t $image -t $latest $context
    }
    if ($LASTEXITCODE -ne 0) { throw "docker build failed for $Name" }

    Write-Host "==> [$Name] push   $image" -ForegroundColor Cyan
    docker push $image
    if ($LASTEXITCODE -ne 0) { throw "docker push failed for $Name" }
    docker push $latest
    if ($LASTEXITCODE -ne 0) { throw "docker push failed for $Name (:latest)" }

    if ($NoRoll) {
        Write-Host "==> [$Name] skip roll (-NoRoll); image pushed" -ForegroundColor DarkYellow
        return
    }
    if (-not (Test-AppExists $app)) {
        Write-Host "==> [$Name] app '$app' not found — run 'terraform apply' in infra/azure to create it. Image is pushed and ready." -ForegroundColor DarkYellow
        return
    }

    Write-Host "==> [$Name] roll   Container Apps revision" -ForegroundColor Cyan
    if ($Name -eq 'frontend') {
        # Terraform owns these env vars too (same values); setting them on the
        # roll keeps the frontend working even if this image lands before the
        # next `terraform apply` stamps them.
        az containerapp update --name $app --resource-group $ResourceGroup --image $image --set-env-vars "PLATFORM_ISSUER=$IssuerUrl" "PORTAL_URL=$PortalUrl" | Out-Null
    }
    else {
        az containerapp update --name $app --resource-group $ResourceGroup --image $image | Out-Null
    }
    if ($LASTEXITCODE -ne 0) { throw "az containerapp update failed for $Name" }

    Write-Host "==> [$Name] done" -ForegroundColor Green
}

if ($Service -in 'backend', 'both') {
    Invoke-DeployService -Name 'backend' -ContextDir 'backend'
}
if ($Service -in 'frontend', 'both') {
    Invoke-DeployService -Name 'frontend' -ContextDir 'frontend'
}

Write-Host ''
Write-Host 'All requested services processed.' -ForegroundColor Green
Write-Host "Frontend: https://demo-frontend.$EnvDomain"
Write-Host "IdP:      $IssuerUrl"
