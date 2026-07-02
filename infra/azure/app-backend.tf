resource "azurerm_container_app" "backend" {
  name                         = "${local.resource_prefix}-backend"
  resource_group_name          = data.azurerm_resource_group.shared.name
  container_app_environment_id = data.azurerm_container_app_environment.shared.id
  revision_mode                = "Single"

  identity {
    type         = "UserAssigned"
    identity_ids = [azurerm_user_assigned_identity.backend.id]
  }

  registry {
    server   = data.azurerm_container_registry.shared.login_server
    identity = azurerm_user_assigned_identity.backend.id
  }

  # Module backends run INTERNAL ingress — reachable only inside the
  # environment (the frontend's nginx /api proxy + Channel 1 service calls) at
  # https://demo-backend.internal.<default domain>. This replaces the GCP
  # IAM-invoker + ID-token arrangement outright; no BACKEND_AUDIENCE needed.
  ingress {
    external_enabled = false
    target_port      = 8080
    transport        = "auto"

    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }

  # Key Vault reference, resolved with the backend's managed identity.
  # Versionless URI form: ACA re-resolves the latest secret version
  # periodically. (The secret itself is owned by platform's TF, so reference
  # it by URI rather than a data source — no secret value enters this state.)
  secret {
    name                = "db-password"
    identity            = azurerm_user_assigned_identity.backend.id
    key_vault_secret_id = "${data.azurerm_key_vault.shared.vault_uri}secrets/${local.resource_prefix}-db-password"
  }

  template {
    min_replicas = 1 # JVM cold start — keep one warm
    max_replicas = 3

    container {
      name  = "backend"
      image = "${data.azurerm_container_registry.shared.login_server}/${local.resource_prefix}-backend:${var.backend_image_tag}"

      # Consumption-plan combos are fixed pairs; 1 vCPU / 2Gi is the nearest
      # step up from the GCP 1 CPU / 1Gi (the JVM appreciates the headroom).
      cpu    = 1.0
      memory = "2Gi"

      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "prod,azure"
      }
      # Demo's app defaults server.port to 8092 (local-dev convention); the
      # container must bind the ingress target port (8080, matching the
      # health probes below). Same quirk as the GCP Cloud Run service.
      env {
        name  = "SERVER_PORT"
        value = "8080"
      }
      env {
        name  = "DB_URL"
        value = "jdbc:postgresql://${data.azurerm_postgresql_flexible_server.shared.fqdn}:5432/demo?sslmode=require"
      }
      env {
        name  = "DB_USER"
        value = "demo_app"
      }
      env {
        name        = "DB_PASSWORD"
        secret_name = "db-password"
      }
      # Central platform IdP (relying-party SSO + metadata registry). The
      # platform backend is the public OIDC issuer; DEMO validates user tokens
      # against its JWKS (PLATFORM_JWKS_URI left unset → <issuer>/oauth2/jwks)
      # and registers its metadata there.
      env {
        name  = "PLATFORM_ISSUER"
        value = local.platform_issuer
      }
      env {
        name  = "PLATFORM_REGISTRY_URL"
        value = local.platform_issuer
      }
      # Presence of this switches on the -javaagent in the backend image
      # entrypoint (see backend/Dockerfile) — OTel auto-instrumentation into
      # the fleet's shared Application Insights.
      env {
        name  = "APPLICATIONINSIGHTS_CONNECTION_STRING"
        value = data.azurerm_application_insights.shared.connection_string
      }

      startup_probe {
        transport               = "HTTP"
        port                    = 8080
        path                    = "/actuator/health"
        initial_delay           = 30
        interval_seconds        = 10
        timeout                 = 5
        failure_count_threshold = 6
      }

      liveness_probe {
        transport               = "HTTP"
        port                    = 8080
        path                    = "/actuator/health"
        initial_delay           = 60
        interval_seconds        = 30
        timeout                 = 5
        failure_count_threshold = 3
      }
    }
  }

  lifecycle {
    # scripts/deploy-azure.ps1 (and CI) roll revisions with SHA-tagged images;
    # don't let terraform reset them to :latest on the next apply.
    ignore_changes = [template[0].container[0].image]
  }

  depends_on = [
    azurerm_role_assignment.backend_acr_pull,
    azurerm_role_assignment.backend_kv_db_password,
  ]
}
