// Azure Bicep root for the DEMO module.
//
// On Azure the PLATFORM repo owns the shared fleet infrastructure (VNet,
// Container Apps environment, ACR, PostgreSQL Flexible Server, Key Vault,
// Log Analytics / Application Insights) — see platform/infra/azure. This
// root only creates the DEMO-scoped pieces: managed identities, role
// assignments, and the two Container Apps. Shared resources are referenced
// by name via `existing` resources; the suffixed names are auto-discovered
// by scripts/deploy-infra-azure.ps1 (fleet-prefix lookup).
//
// DEMO-specific fleet prerequisites already provisioned by the platform root:
//   - Key Vault secret      demo-db-password
//   - Postgres database     demo (+ role demo_app via the <fleet>-db-init job)
//
// No Service Bus wiring: demo runs without the event binder on Azure today
// (a csnx.events "demo" subscription already exists topic-side for when the
// pom->demo CDC feed is enabled — that needs the SB env vars + Data
// Receiver/Sender grants mirroring pom's).

targetScope = 'resourceGroup'

@description('Azure region for DEMO-owned resources (must match the shared fleet\'s region).')
param location string = 'southafricanorth'

@description('Short name prefixing DEMO-owned resources.')
@minLength(3)
@maxLength(20)
param appName string = 'demo'

// --- Shared fleet references (owned by platform/infra/azure) ---------------

@description('Shared Container Apps environment name.')
param containerAppEnvironmentName string = 'csnx-aca-env'

@description('Shared Application Insights component name.')
param appInsightsName string = 'csnx-appinsights'

@description('Shared container registry name (random-suffixed).')
param acrName string

@description('Shared Key Vault name (random-suffixed). Holds demo-db-password.')
param keyVaultName string

@description('Shared PostgreSQL Flexible Server name (random-suffixed).')
param postgresServerName string

// --- DEMO deployment knobs ----------------------------------------------------

@description('Central platform IdP issuer URL. Empty = the platform backend\'s default FQDN on the shared environment.')
param platformIssuer string = ''

@description('Image tag for the backend container in ACR (redeploys re-read the running tag).')
param backendImageTag string = 'latest'

@description('Image tag for the frontend container in ACR (same redeploy contract).')
param frontendImageTag string = 'latest'

// --- Shared fleet resources ---------------------------------------------------

resource acaEnv 'Microsoft.App/managedEnvironments@2025-01-01' existing = {
  name: containerAppEnvironmentName
}

resource acr 'Microsoft.ContainerRegistry/registries@2025-04-01' existing = {
  name: acrName
}

resource vault 'Microsoft.KeyVault/vaults@2024-11-01' existing = {
  name: keyVaultName
}

resource dbPasswordSecret 'Microsoft.KeyVault/vaults/secrets@2024-11-01' existing = {
  parent: vault
  name: '${appName}-db-password'
}

resource postgres 'Microsoft.DBforPostgreSQL/flexibleServers@2024-08-01' existing = {
  name: postgresServerName
}

resource appInsights 'Microsoft.Insights/components@2020-02-02' existing = {
  name: appInsightsName
}

var envDomain = acaEnv.properties.defaultDomain
var backendUrl = 'https://${appName}-backend.internal.${envDomain}'
var frontendUrl = 'https://${appName}-frontend.${envDomain}'
var resolvedIssuer = empty(platformIssuer) ? 'https://platform-backend.${envDomain}' : platformIssuer
var portalUrl = 'https://platform-frontend.${envDomain}'

// --- Identities + least-privilege grants --------------------------------------
//   backend : pull images, read its single db-password secret
//   frontend: pull images only

@description('Tags stamped on the module-owned resources (fleet convention).')
param tags object = {
  system: 'csnx'
  environment: 'dev'
  managedBy: 'bicep'
}

resource backendIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2024-11-30' = {
  name: '${appName}-backend'
  location: location
  tags: tags
}

resource frontendIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2024-11-30' = {
  name: '${appName}-frontend'
  location: location
  tags: tags
}

var roleIds = {
  acrPull: '7f951dda-4ed3-4680-a7ca-43fe172d538d'
  keyVaultSecretsUser: '4633458b-17de-408a-b874-0445c86b69e6'
}

resource backendAcrPull 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(acr.id, backendIdentity.name, roleIds.acrPull)
  scope: acr
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', roleIds.acrPull)
    principalId: backendIdentity.properties.principalId
    principalType: 'ServicePrincipal'
  }
}

resource frontendAcrPull 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(acr.id, frontendIdentity.name, roleIds.acrPull)
  scope: acr
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', roleIds.acrPull)
    principalId: frontendIdentity.properties.principalId
    principalType: 'ServicePrincipal'
  }
}

// Scoped to the ONE secret demo reads, not the whole shared vault.
resource backendKvSecret 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(dbPasswordSecret.id, backendIdentity.name, roleIds.keyVaultSecretsUser)
  scope: dbPasswordSecret
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', roleIds.keyVaultSecretsUser)
    principalId: backendIdentity.properties.principalId
    principalType: 'ServicePrincipal'
  }
}

// --- Backend -------------------------------------------------------------------

resource backendApp 'Microsoft.App/containerApps@2025-01-01' = {
  name: '${appName}-backend'
  location: location
  tags: tags
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: {
      '${backendIdentity.id}': {}
    }
  }
  properties: {
    environmentId: acaEnv.id
    configuration: {
      activeRevisionsMode: 'Single'
      // INTERNAL ingress — the fleet's standard posture for module backends.
      ingress: {
        external: false
        targetPort: 8080
        transport: 'auto'
        traffic: [
          {
            latestRevision: true
            weight: 100
          }
        ]
      }
      registries: [
        {
          server: acr.properties.loginServer
          identity: backendIdentity.id
        }
      ]
      secrets: [
        {
          name: 'db-password'
          keyVaultUrl: '${vault.properties.vaultUri}secrets/${appName}-db-password'
          identity: backendIdentity.id
        }
      ]
    }
    template: {
      scale: {
        minReplicas: 1 // JVM cold start — keep one warm
        maxReplicas: 3
      }
      containers: [
        {
          name: 'backend'
          image: '${acr.properties.loginServer}/${appName}-backend:${backendImageTag}'
          resources: {
            cpu: json('1.0')
            memory: '2Gi'
          }
          env: [
            {
              name: 'SPRING_PROFILES_ACTIVE'
              value: 'prod,azure'
            }
            {
              // Demo's app defaults server.port to 8092 (local-dev
              // convention); the container must bind the ingress target port
              // (8080, matching the health probes).
              name: 'SERVER_PORT'
              value: '8080'
            }
            {
              name: 'DB_URL'
              value: 'jdbc:postgresql://${postgres.properties.fullyQualifiedDomainName}:5432/${appName}?sslmode=require'
            }
            {
              name: 'DB_USER'
              value: '${appName}_app'
            }
            {
              name: 'DB_PASSWORD'
              secretRef: 'db-password'
            }
            {
              // Central platform IdP; JWKS defaults to <issuer>/oauth2/jwks.
              name: 'PLATFORM_ISSUER'
              value: resolvedIssuer
            }
            {
              name: 'PLATFORM_REGISTRY_URL'
              value: resolvedIssuer
            }
            {
              name: 'APPLICATIONINSIGHTS_CONNECTION_STRING'
              value: appInsights.properties.ConnectionString
            }
            {
              // cloud_RoleName in Application Insights. Without it every
              // backend on the fleet reports as "unknown_service:java" and
              // Application Map / per-module failure panels can't tell
              // them apart.
              name: 'OTEL_SERVICE_NAME'
              value: '${appName}-backend'
            }
          ]
          probes: [
            {
              type: 'Startup'
              httpGet: {
                path: '/actuator/health'
                port: 8080
              }
              initialDelaySeconds: 30
              periodSeconds: 10
              timeoutSeconds: 5
              failureThreshold: 6
            }
            {
              type: 'Liveness'
              httpGet: {
                path: '/actuator/health'
                port: 8080
              }
              initialDelaySeconds: 60
              periodSeconds: 30
              timeoutSeconds: 5
              failureThreshold: 3
            }
            {
              // Traffic only reaches replicas that answer - matters during
              // revision rolls and scale-out.
              type: 'Readiness'
              httpGet: {
                path: '/actuator/health'
                port: 8080
              }
              periodSeconds: 10
              timeoutSeconds: 5
              failureThreshold: 3
            }
          ]
        }
      ]
    }
  }
  dependsOn: [
    backendAcrPull
    backendKvSecret
  ]
}

// --- Frontend ------------------------------------------------------------------

resource frontendApp 'Microsoft.App/containerApps@2025-01-01' = {
  name: '${appName}-frontend'
  location: location
  tags: tags
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: {
      '${frontendIdentity.id}': {}
    }
  }
  properties: {
    environmentId: acaEnv.id
    configuration: {
      activeRevisionsMode: 'Single'
      ingress: {
        external: true
        targetPort: 80
        transport: 'auto'
        traffic: [
          {
            latestRevision: true
            weight: 100
          }
        ]
      }
      registries: [
        {
          server: acr.properties.loginServer
          identity: frontendIdentity.id
        }
      ]
    }
    template: {
      scale: {
        minReplicas: 1
        maxReplicas: 3
      }
      containers: [
        {
          name: 'frontend'
          image: '${acr.properties.loginServer}/${appName}-frontend:${frontendImageTag}'
          resources: {
            cpu: json('0.25')
            memory: '0.5Gi'
          }
          env: [
            {
              // nginx proxies /api to the backend's INTERNAL ingress FQDN.
              name: 'BACKEND_URL'
              value: backendUrl
            }
            {
              name: 'PLATFORM_ISSUER'
              value: resolvedIssuer
            }
            {
              name: 'PORTAL_URL'
              value: portalUrl
            }
          ]
          probes: [
            {
              type: 'Startup'
              httpGet: {
                path: '/'
                port: 80
              }
              initialDelaySeconds: 5
              periodSeconds: 5
              timeoutSeconds: 3
              failureThreshold: 6
            }
            {
              // Traffic only reaches replicas that answer - matters during
              // revision rolls and scale-out.
              type: 'Readiness'
              httpGet: {
                path: '/'
                port: 80
              }
              periodSeconds: 10
              timeoutSeconds: 3
              failureThreshold: 3
            }
          ]
        }
      ]
    }
  }
  dependsOn: [
    frontendAcrPull
  ]
}

@description('Public HTTPS URL of the DEMO portal.')
output frontendUrl string = frontendUrl

@description('Internal-ingress HTTPS URL of the DEMO backend.')
output backendUrl string = backendUrl

@description('Platform IdP issuer this deployment is wired to.')
output platformIssuer string = resolvedIssuer
