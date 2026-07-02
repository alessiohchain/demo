resource "azurerm_container_app" "frontend" {
  name                         = "${local.resource_prefix}-frontend"
  resource_group_name          = data.azurerm_resource_group.shared.name
  container_app_environment_id = data.azurerm_container_app_environment.shared.id
  revision_mode                = "Single"

  identity {
    type         = "UserAssigned"
    identity_ids = [azurerm_user_assigned_identity.frontend.id]
  }

  registry {
    server   = data.azurerm_container_registry.shared.login_server
    identity = azurerm_user_assigned_identity.frontend.id
  }

  ingress {
    external_enabled = true
    target_port      = 80
    transport        = "auto"

    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }

  template {
    min_replicas = 1
    max_replicas = 3

    container {
      name  = "frontend"
      image = "${data.azurerm_container_registry.shared.login_server}/${local.resource_prefix}-frontend:${var.frontend_image_tag}"

      cpu    = 0.25
      memory = "0.5Gi"

      # nginx proxies /api to the backend's INTERNAL ingress FQDN inside the
      # environment. BACKEND_AUDIENCE stays unset — that env only exists for
      # the GCP ID-token path in docker-entrypoint.sh; internal ingress
      # replaces it here.
      env {
        name  = "BACKEND_URL"
        value = "https://${azurerm_container_app.backend.ingress[0].fqdn}"
      }

      startup_probe {
        transport               = "HTTP"
        port                    = 80
        path                    = "/"
        initial_delay           = 5
        interval_seconds        = 5
        timeout                 = 3
        failure_count_threshold = 6
      }
    }
  }

  lifecycle {
    # Same as the backend: image rolls happen outside terraform.
    ignore_changes = [template[0].container[0].image]
  }

  depends_on = [azurerm_role_assignment.frontend_acr_pull]
}
