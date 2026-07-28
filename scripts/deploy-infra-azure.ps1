<#
.SYNOPSIS
  Deploy the DEMO-scoped Azure infrastructure (infra/azure/main.bicep) with
  `az deployment group` -- the Bicep sibling of deploy-azure.ps1, which owns
  images and revision rolls.

.DESCRIPTION
  The shared fleet (VNet, ACA environment, ACR, Postgres, Key Vault) is owned
  by the PLATFORM repo and must exist first. This wrapper auto-discovers the
  random-suffixed shared resource names from the resource group (fleet-prefix
  lookup), so nothing needs copying from platform outputs by hand. Image tags
  of already-running apps are re-read so an infra redeploy never resets a
  SHA-tagged revision roll back to :latest.

  First-time bootstrap:
    1. .\scripts\deploy-azure.ps1 -NoRoll        # build + push images
    2. .\scripts\deploy-infra-azure.ps1          # creates the demo apps
  Later infra changes: edit infra/azure/main.bicep, re-run (use -WhatIf first).

.PARAMETER ResourceGroup
  Target resource group. Default: $env:CSNX_RESOURCE_GROUP, else 'csnx-rg'.

.PARAMETER FleetName
  Fleet prefix of the shared resources. Default: $env:CSNX_FLEET_NAME, else 'csnx'.

.PARAMETER Location
  Region for the DEMO-owned resources (must match the shared fleet's region).

.PARAMETER WhatIf
  Run `az deployment group what-if` instead of deploying.

.PARAMETER IssuerUrl
  Custom-domain platform IdP issuer (see infra/azure/main.bicep platformIssuer).
#>
[CmdletBinding()]
param(
    [string]$ResourceGroup = $(if ($env:CSNX_RESOURCE_GROUP) { $env:CSNX_RESOURCE_GROUP } else { 'csnx-rg' }),
    [string]$FleetName = $(if ($env:CSNX_FLEET_NAME) { $env:CSNX_FLEET_NAME } else { 'csnx' }),
    [string]$Location = 'southafricanorth',

    [switch]$WhatIf,

    [string]$IssuerUrl = ''
)

$ErrorActionPreference = 'Stop'

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$Template = Join-Path $RepoRoot 'infra\azure\main.bicep'

az account show --query id -o tsv | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Not logged in -- run: az login' }

# --- Auto-discover the shared fleet's random-suffixed resource names --------

function Find-FleetResource {
    param(
        [Parameter(Mandatory)] [string]$Kind,
        [Parameter(Mandatory)] [string]$ListCommand,
        [Parameter(Mandatory)] [string]$NamePrefix
    )
    $name = Invoke-Expression "$ListCommand --resource-group `"$ResourceGroup`" --query `"[?starts_with(name, '$NamePrefix')].name | [0]`" -o tsv"
    if ($LASTEXITCODE -ne 0 -or -not $name) {
        throw "No '$NamePrefix*' $Kind found in $ResourceGroup -- run the PLATFORM repo's scripts/deploy-infra-azure.ps1 first."
    }
    return $name
}

$AcrName = Find-FleetResource -Kind 'ACR' -ListCommand 'az acr list' -NamePrefix $FleetName
$KeyVaultName = Find-FleetResource -Kind 'Key Vault' -ListCommand 'az keyvault list' -NamePrefix "$FleetName-kv-"
$PostgresName = Find-FleetResource -Kind 'Postgres server' -ListCommand 'az postgres flexible-server list' -NamePrefix "$FleetName-pg-"

# --- Preserve rolled image tags (deploy-azure.ps1 rolls SHA tags) -----------

function Get-CurrentImageTag {
    param([Parameter(Mandatory)] [string]$App)
    $eap = $ErrorActionPreference; $ErrorActionPreference = 'SilentlyContinue'
    $image = az containerapp show --name $App --resource-group $ResourceGroup `
        --query 'properties.template.containers[0].image' -o tsv 2>$null
    $ErrorActionPreference = $eap
    if ($LASTEXITCODE -ne 0 -or -not $image) { return 'latest' }
    return ($image -split ':')[-1]
}

$BackendTag = Get-CurrentImageTag 'demo-backend'
$FrontendTag = Get-CurrentImageTag 'demo-frontend'

# --- Deploy ------------------------------------------------------------------

$DeploymentName = "$FleetName-demo-$(Get-Date -Format 'yyyyMMddHHmmss')"

$params = @(
    "location=$Location"
    "acrName=$AcrName"
    "keyVaultName=$KeyVaultName"
    "postgresServerName=$PostgresName"
    "platformIssuer=$IssuerUrl"
    "backendImageTag=$BackendTag"
    "frontendImageTag=$FrontendTag"
)

if ($WhatIf) {
    Write-Host "==> [what-if] $Template -> $ResourceGroup" -ForegroundColor Cyan
    az deployment group what-if --resource-group $ResourceGroup --template-file $Template --parameters @params
    if ($LASTEXITCODE -ne 0) { throw 'az deployment group what-if failed' }
    return
}

Write-Host "==> [deploy] $Template -> $ResourceGroup (name=$DeploymentName)" -ForegroundColor Cyan
az deployment group create --resource-group $ResourceGroup --name $DeploymentName `
    --template-file $Template --parameters @params --output none
if ($LASTEXITCODE -ne 0) { throw 'az deployment group create failed' }

Write-Host '==> [deploy] succeeded -- outputs:' -ForegroundColor Green
az deployment group show --resource-group $ResourceGroup --name $DeploymentName `
    --query 'properties.outputs.{frontendUrl:frontendUrl.value, backendUrl:backendUrl.value, platformIssuer:platformIssuer.value}' -o table
